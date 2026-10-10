"""Rewriting the calls that ask the phone where it is.

Without root there is no Xposed, and without Xposed there is nothing to hook at
runtime -- so the calls are redirected in the bytecode instead. Each one becomes
a static call into the mod:

    invoke-virtual {v0}, Landroid/telephony/TelephonyManager;->getSimCountryIso()Ljava/lang/String;
    invoke-static  {v0}, Lcat/narezany/margyt/Region;->getSimCountryIso(Landroid/telephony/TelephonyManager;)Ljava/lang/String;

The instruction format (35c), the register count and the return type all match,
so nothing around the call has to be renumbered: the receiver just becomes the
first argument.

Targets are found by signature, never by offset or by file name, so a new
TikTok release does not move them. Of the apk's fifty-two dex files, the four
or five that mention telephony at all are the only ones taken apart; the rest
are copied across untouched, which is the difference between a build that takes
minutes and one that takes hours.
"""

from __future__ import annotations

import os
import re
import shutil
import struct
import subprocess
from typing import Dict, List, Optional, Tuple

TELEPHONY = "Landroid/telephony/TelephonyManager;"
REGION = "Lcat/narezany/margyt/Region;"
ACCENT = "Lcat/narezany/margyt/Accent;"
DOWNLOAD = "Lcat/narezany/margyt/Download;"
FEED = "Lcat/narezany/margyt/Feed;"

# TikTok's own models. Every name here is a real one, read out of the apk's
# method and field tables rather than guessed, and each is answered by a static
# of ours with the receiver moved into the first argument -- the same 35c
# instruction, the same register count, the same return type.
VIDEO = "Lcom/ss/android/ugc/aweme/feed/model/Video;"
ACL = "Lcom/ss/android/ugc/aweme/feed/model/ACLCommonShare;"
AWEME = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
USER = "Lcom/ss/android/ugc/aweme/profile/model/User;"
PROFILE_USER = "Lcom/ss/android/ugc/profile/platform/base/data/UserProfileInfo;"
VIDEO_CONTROL = "Lcom/ss/android/ugc/aweme/feed/model/VideoControl;"
FEED_ITEM_LIST = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;"
PHOTO_IMAGE = "Lcom/ss/android/ugc/aweme/feed/model/PhotoModeImageUrlModel;"
ACCOUNT_SERVICE = "Lcom/ss/android/ugc/aweme/IAccountUserService;"
ACCOUNT = "Lcat/narezany/margyt/Account;"
COMMENTS = "Lcat/narezany/margyt/Comments;"
RAIL = "Lcat/narezany/margyt/FeedRail;"
RAIL_CLASSES = (
    "Lcom/ss/android/ugc/aweme/feed/assem/digg/VideoDiggAssem;",
    "Lcom/ss/android/ugc/aweme/feed/assem/videocomment/VideoCommentAssem;",
    "Lcom/ss/android/ugc/aweme/feed/favorite/VideoFavoriteAssem;",
    "Lcom/ss/android/ugc/aweme/feed/assem/share/VideoShareAssem;",
)
PLAYER_CONTROLLER="Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;"
REPOST_CLASS="Lcom/ss/android/ugc/aweme/upvote/detail/videorepost/VideoRepostAssem;"
AVATAR_CLASS="Lcom/ss/android/ugc/aweme/feed/assem/avatar/FeedAvatarDefaultAssem;"
PROFILE_CONTAINER="Lcom/ss/android/ugc/profile/platform/base/component/BaseContainerComponent;"
PROFILE_HEADER_CONTAINER="Lcom/ss/android/ugc/profile/platform/business/header/base/HeaderBaseContainerComponent;"
MESSAGE_CLASSES=("Lcom/ss/android/ugc/aweme/im/sdk/chat/messagelist/skeleton/cell/IMSkeletonLayoutReusedUISlotAssem;","Lcom/ss/android/ugc/aweme/im/sdk/chat/messagelist/skeleton/cell/SkeletonLayoutReusedUISlotAssem;")
PUSH_GUIDE="Lcom/ss/android/ugc/aweme/unifiedauth/push/UniAuthGuidePopupServiceImpl;"
FRIENDS_RESPONSE="Lcom/ss/android/ugc/aweme/relation/model/FriendsListResponse;"
COMMENT_IMAGE = "Lcom/ss/android/ugc/aweme/comment/model/CommentImageStruct;"
COMMENT = "Lcom/ss/android/ugc/aweme/comment/model/Comment;"
COMMENT_STICKER = "Lcom/ss/android/ugc/aweme/comment/model/CommentStickerStruct;"
WATERMARK = "Lcat/narezany/margyt/Watermark;"
FLAGS = "Lcat/narezany/margyt/Flags;"
SOUND = "Lcat/narezany/margyt/Sound;"
BADGE = "Lcat/narezany/margyt/Badge;"
AVATARS = "Lcat/narezany/margyt/Avatars;"
STICKERS = "Lcat/narezany/margyt/Stickers;"
STREAKS = "Lcat/narezany/margyt/Streaks;"
STREAK_DATA = "Lcom/ss/android/ugc/aweme/im/streak/api/StreakData;"
STREAK_SERVICE = "Lcom/ss/android/ugc/aweme/im/streak/api/IStreakService;"
STICKER_ITEM = "Lcom/ss/android/ugc/aweme/im/common/model/StickerItem;"
STICKER_IMAGE = "Lcom/ss/android/ugc/aweme/im/common/model/StickerImage;"
STICKER_BASE = "Lcom/ss/android/ugc/aweme/im/common/model/StickerBase;"
STICKER_CLICK = "Lcom/ss/android/ugc/aweme/im/messagelist/api/ability/MessageListStickerClickAbility;"
STICKER_TEMPLATE = "Lcom/ss/android/ugc/aweme/im/message/template/card/StickerTemplate;"
TEXT_VIEW = "Landroid/widget/TextView;"
# TikTok writes almost everything with its own text view, and a call is
# compiled against the type it is written on -- so a rule that names
# TextView never matches one of these. The mod takes them as TextView all
# the same, which is what they are.
TUX_TEXT = "Lcom/bytedance/tux/input/TuxTextView;"
CHAR_SEQUENCE = "Ljava/lang/CharSequence;"
SEEKBAR = "Lcat/narezany/margyt/Seekbar;"
FONTS = "Lcat/narezany/margyt/Fonts;"
TYPEFACE = "Landroid/graphics/Typeface;"
PAINT_CLASS = "Landroid/graphics/Paint;"
MUSIC = "Lcom/ss/android/ugc/aweme/music/model/Music;"

# A method whose name is real on a class whose name is not. Matching the owner
# would mean writing down an obfuscated name that changes every release, so the
# owner is left open and the receiver arrives as a plain Object -- which is why
# the mod calls the original back by reflection rather than directly.
WILD_SOURCES: List[Tuple[str, str, str, str]] = [
    ("setSeekBarShowType", "(I)V", "(Ljava/lang/Object;I)V", SEEKBAR),
]

# A sticker touched in a conversation.
#
# The interface is MessageListStickerClickAbility and the type it is handed is
# StickerTemplate, both real names -- but a call site names whatever static
# type it is holding, which is the class implementing the interface, so the
# owner is left open. What is written down instead is each method's exact
# shape, obfuscated parameter types and all: those move between releases, and a
# release that moves them leaves the rules matching nothing, which is a feature
# that does not appear rather than an app that breaks.
#
# Everything but the sticker itself arrives as Object -- a reference is a
# reference as far as the verifier is concerned -- which is what keeps a fifth
# obfuscated name out of the mod's own source.
_TAP = "Landroid/view/View;LX/1CpA;LX/13Vd;" + STICKER_TEMPLATE
_SHEET = ("Landroid/view/View;Landroidx/fragment/app/FragmentManager;LX/13Vd;"
          + STICKER_TEMPLATE)
_INNER = "LX/1CpA;LX/13Vd;" + STICKER_TEMPLATE
_CORNER = "Lcom/ss/android/ugc/aweme/views/RoundingCornerLayout;" + STICKER_TEMPLATE
_ANY = "Ljava/lang/Object;"

WILD_SOURCES += [
    ("UP", "(%s)V" % _TAP, "(%s%s)V" % (_ANY * 4, STICKER_TEMPLATE), STICKERS),
    ("tR1", "(%s)V" % _TAP, "(%s%s)V" % (_ANY * 4, STICKER_TEMPLATE), STICKERS),
    ("dy1", "(%s)V" % _SHEET, "(%s%s)V" % (_ANY * 4, STICKER_TEMPLATE), STICKERS),
    ("Yt1", "(%s)V" % _INNER, "(%s%s)V" % (_ANY * 3, STICKER_TEMPLATE), STICKERS),
    ("XQ1", "(%s)V" % _CORNER, "(%s%s)V" % (_ANY * 2, STICKER_TEMPLATE), STICKERS),
]

# TikTok's A/B facade. The class name is real; the method names are what the
# obfuscator made of them in 46.9.42, one per type, each taking the flag's name
# and what to answer when the server said nothing. A release that renames them
# leaves the rules matching nothing, which turns the overrides off and breaks
# no part of the app.
SETTINGS_MANAGER = "Lcom/bytedance/ies/abmock/SettingsManager;"
AB_READERS: List[Tuple[str, str, str]] = [
    ("LIZ", "Ljava/lang/String;Z", "Z"),
    ("LJ", "Ljava/lang/String;I", "I"),
    ("LJFF", "Ljava/lang/String;J", "J"),
    ("LJI", "Ljava/lang/String;Ljava/lang/String;", "Ljava/lang/String;"),
    ("LIZJ", "Ljava/lang/String;F", "F"),
    ("LIZIZ", "Ljava/lang/String;D", "D"),
]
CANVAS = "Landroid/graphics/Canvas;"

# Rules that apply inside one class and nowhere else, found by a string the
# class carries rather than by its name.
#
# The stamp on a saved picture is assembled on the phone -- text and a logo
# drawn into a bitmap of their own -- and then put on the picture with a single
# Canvas.drawBitmap. Redirecting every drawBitmap in the apk would be absurd;
# redirecting the one in the class that builds the label is a rewrite of a
# single instruction. The class is X.0Hnc in 46.9.42 and will be something else
# in 46.10, but the marker in its template stays.
#
# anchor, owner, method, its descriptor, ours, the class ours lives in
# A static TikTok's obfuscation renamed but could not disguise: what it takes
# is the framework's own types and the app's own models, and no other method in
# the apk takes that. The build finds the one class that declares it, writes
# the name down for the mod to hand the call back through, and rewrites every
# call to it. Nothing obfuscated is written here, and a release that shuffles
# the names is simply found again on the next build.
#
# label, the descriptor to look for, what the mod calls it, where that lives
COMMENT_TAP = ("(Landroid/view/View;%s ZLjava/lang/String;Ljava/util/Map;"
               "Ljava/lang/String;)V" % COMMENT_STICKER).replace("; Z", ";Z")

DISCOVERED_STATICS: List[Tuple[str, str, str, str]] = [
    # a sticker in a comment, tapped. In a conversation the tap goes through an
    # interface the mod can name; here it goes to a static on a class with no
    # name worth writing down -- but it takes the view that was touched and the
    # sticker that was in it, and nothing else in the apk takes that pair.
    ("comment sticker tapped", COMMENT_TAP, "stickerTapped", COMMENTS),
]

STICKER_ITEM_D = "Lcom/ss/android/ugc/aweme/im/common/model/StickerItem;"
FUNCTION0 = "Lkotlin/jvm/functions/Function0;"

#: the sheet that opens on a comment's sticker -- Share, Save, Use. Found the
#: same way, by what it takes: a sticker and the view it was touched in. The
#: receiver comes through as an Object and the call is handed back by
#: reflection, so the class it lives on is never written down here either.
COMMENT_SHEET = ("(Ljava/lang/String;%s Landroid/view/View;ZLjava/lang/String;"
                 "Ljava/util/Map;%s %s %s)V"
                 % (STICKER_ITEM_D, FUNCTION0, FUNCTION0, FUNCTION0)).replace(" ", "")

DISCOVERED_VIRTUALS: List[Tuple[str, str, str, str]] = [
    ("comment sticker sheet", COMMENT_SHEET, "stickerSheet", COMMENTS),
]

# filled in by the build: label -> (owner, the name it carries this release)
FOUND: Dict[str, Tuple[str, str]] = {}


VIEW = "Landroid/view/View;"
LONG_CLICK = "Landroid/view/View$OnLongClickListener;"

ANCHORED_SOURCES: List[Tuple[str, str, str, str, str, str]] = [
    # A sticker in a comment already answers a long press -- TikTok sets its
    # own listener on it. So the mod does not add a gesture, it wraps the one
    # that is there: the press still does what it did, and the offer to save
    # appears beside it.
    #
    # The class is anchored by a line TikTok logs while binding a sticker,
    # which no other class in the apk carries. Anchoring matters here more than
    # anywhere: `setOnLongClickListener` is the framework's, and rewriting
    # every call to it would mean wrapping several thousand unrelated views.
    ("bindSticker: ", VIEW, "setOnLongClickListener",
     "(%s)V" % LONG_CLICK, "(%s%s)V" % (VIEW, LONG_CLICK), COMMENTS),
    # TikTok 47.0.42 has one long-press listener on the comment-row base
    # cell.  The class descriptor is a stable dex string and restricts this
    # rewrite to that row; other long presses (video, profiles, stickers) are
    # deliberately not touched.
    ("Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
     VIEW, "setOnLongClickListener", "(%s)V" % LONG_CLICK,
     "(%s%s)V" % (VIEW, LONG_CLICK), COMMENTS),
    ("[tiktok_logo]", CANVAS, "drawBitmap",
     "(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V",
     "(%sLandroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V" % CANVAS, WATERMARK),
]

# Most anchors are ordinary text literals.  A class descriptor is also an
# ordinary dex string, though, so it can occur in synthetic listener classes
# that merely *refer* to the cell.  This one must mean the smali declaration
# itself, never a const-class or method parameter elsewhere.
CLASS_ANCHORED_SOURCES = {
    "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
}

# TikTok 47.2.41's feed touch listener. The callback below is inserted only in
# this one method after the multi-touch early return; no global touch dispatch
# is rewritten.
SPEED_TOUCH_CLASS = "LY/ATListenerS452S0100000_16;"
SPEED_EDGE_CLASS = "LX/16wu;"
SPEED_EDGE_METHOD = ".method public final onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z"
SPEED_TOUCH_METHOD = (
    ".method public static final onTouch$8(LY/ATListenerS452S0100000_16;"
    "Landroid/view/View;Landroid/view/MotionEvent;)Z"
)
SPEED_TOUCH_TARGET = (
    "Lcat/narezany/margyt/SpeedGesture;->onTouch(Ljava/lang/Object;"
    "Landroid/view/View;Landroid/view/MotionEvent;)Z"
)
SPEED_TOUCH_HOOK = """\
    invoke-static {p0, p1, p2}, Lcat/narezany/margyt/SpeedGesture;->onTouch(Ljava/lang/Object;Landroid/view/View;Landroid/view/MotionEvent;)Z
    move-result v0
    if-eqz v0, :ttcuz_speed_touch_continue
    const/4 v0, 0x1
    return v0
:ttcuz_speed_touch_continue
"""
STRING = "Ljava/lang/String;"
URL_MODEL = "Lcom/ss/android/ugc/aweme/base/model/UrlModel;"
BOXED_BOOLEAN = "Ljava/lang/Boolean;"
LIST = "Ljava/util/List;"

# owner, method, its descriptor, ours, the class ours lives in
SUBSCRIPTIONS = "Landroid/telephony/SubscriptionManager;"

MODEL_SOURCES: List[Tuple[str, str, str, str, str]] = [
    # How many SIMs the system says are in the phone. Answering the six
    # questions about the card is not enough on a phone with no card in it:
    # the app asks how many there are first, gets nothing, and never asks the
    # rest. These say one, which is what a phone with a card says.
    (TELEPHONY, "getPhoneCount", "()I", "(%s)I" % TELEPHONY, REGION),
    (TELEPHONY, "getActiveModemCount", "()I", "(%s)I" % TELEPHONY, REGION),
    (TELEPHONY, "getPhoneType", "()I", "(%s)I" % TELEPHONY, REGION),
    (SUBSCRIPTIONS, "getActiveSubscriptionInfoCount", "()I",
     "(%s)I" % SUBSCRIPTIONS, REGION),
    # the save button's address: stamped, and the clean one beside it
    (VIDEO, "getDownloadAddr", "()%s" % URL_MODEL, "(%s)%s" % (VIDEO, URL_MODEL), DOWNLOAD),
    # what the post says may be done with it, which TikTok reads before it
    # offers a download at all
    (ACL, "getCode", "()I", "(%s)I" % ACL, DOWNLOAD),
    (ACL, "getShowType", "()I", "(%s)I" % ACL, DOWNLOAD),
    (ACL, "getTranscode", "()I", "(%s)I" % ACL, DOWNLOAD),
    # the ban on saving, on the post and on the account that made it
    (AWEME, "isPreventDownload", "()Z", "(%s)Z" % AWEME, DOWNLOAD),
    (USER, "isPreventDownload", "()Z", "(%s)Z" % USER, DOWNLOAD),
    # the page of the feed, before anything has looked at it
    (FEED_ITEM_LIST, "getItems", "()%s" % LIST, "(%s)%s" % (FEED_ITEM_LIST, LIST), FEED),
    # who is signed in. The mod does not ask -- there is no unobfuscated way to
    # reach the service -- so it listens instead: the app asks often enough,
    # and the answer goes past on its way back.
    (ACCOUNT_SERVICE, "getCurUser", "()%s" % USER,
     "(%s)%s" % (ACCOUNT_SERVICE, USER), ACCOUNT),
    (ACCOUNT_SERVICE, "getCurUserId", "()%s" % STRING,
     "(%s)%s" % (ACCOUNT_SERVICE, STRING), ACCOUNT),
    (ACCOUNT_SERVICE, "getCurSecUserId", "()%s" % STRING,
     "(%s)%s" % (ACCOUNT_SERVICE, STRING), ACCOUNT),
    # which sticker a comment is carrying, read as the comment is bound: the
    # view gets its long press wrapped a moment later, and this is what says
    # what that view is showing
    (COMMENT, "getStickerStruct", "()%s" % COMMENT_STICKER,
     "(Ljava/lang/Object;)%s" % COMMENT_STICKER, COMMENTS),
    (COMMENT_IMAGE, "getCropUrl", "()%s" % URL_MODEL,
     "(%s)%s" % (COMMENT_IMAGE, URL_MODEL), COMMENTS),
    (COMMENT_IMAGE, "getOriginUrl", "()%s" % URL_MODEL,
     "(%s)%s" % (COMMENT_IMAGE, URL_MODEL), COMMENTS),
    # a name, and every place that writes one. The mark rides out on the name
    # and becomes a picture on the way into the view that shows it
    (USER, "getUniqueId", "()Ljava/lang/String;", "(Lcom/ss/android/ugc/aweme/profile/model/User;)Ljava/lang/String;", BADGE),
    (USER, "getNickname", "()Ljava/lang/String;",
     "(%s)Ljava/lang/String;" % USER, BADGE),
    ("Lcom/ss/android/ugc/aweme/im/contacts/api/model/IMUser;", "getDisplayName", "()Ljava/lang/String;",
     "(Ljava/lang/Object;)Ljava/lang/String;", BADGE),
    ("Lcom/ss/android/ugc/aweme/im/common/model/BaseContact;", "getNickName", "()Ljava/lang/String;",
     "(Ljava/lang/Object;)Ljava/lang/String;", BADGE),
    ("Lcom/ss/android/ugc/aweme/im/contacts/api/model/IMContact;", "getDisplayName", "()Ljava/lang/String;",
     "(Ljava/lang/Object;)Ljava/lang/String;", BADGE),
    ("Lcom/ss/android/ugc/aweme/im/contacts/api/model/IMUser;", "getNickName", "()Ljava/lang/String;",
     "(Ljava/lang/Object;)Ljava/lang/String;", BADGE),
    # and the model a profile switches to once it has finished loading, which
    # is why a badge used to appear while the profile loaded and then go away
    (PROFILE_USER, "getNickname", "()Ljava/lang/String;",
     "(%s)Ljava/lang/String;" % PROFILE_USER, BADGE),

    # the avatar, at the sizes the app actually asks for: the mod does not need
    # to know whose profile is open, only which picture was last wanted
    (USER, "getAvatarLarger", "()%s" % URL_MODEL, "(%s)%s" % (USER, URL_MODEL), AVATARS),
    (USER, "getAvatar300", "()%s" % URL_MODEL, "(%s)%s" % (USER, URL_MODEL), AVATARS),
    (USER, "getAvatarMedium", "()%s" % URL_MODEL, "(%s)%s" % (USER, URL_MODEL), AVATARS),
    # the typeface, wherever TikTok chooses one for itself. Most views never
    # do -- they inherit it -- and those are reached on the way text goes in,
    # which the mod is already standing in for the badges
    (TEXT_VIEW, "setTypeface", "(%s)V" % TYPEFACE,
     "(%s%s)V" % (TEXT_VIEW, TYPEFACE), FONTS),
    (TEXT_VIEW, "setTypeface", "(%sI)V" % TYPEFACE,
     "(%s%sI)V" % (TEXT_VIEW, TYPEFACE), FONTS),
    (TUX_TEXT, "setTypeface", "(%s)V" % TYPEFACE,
     "(%s%s)V" % (TEXT_VIEW, TYPEFACE), FONTS),
    (TUX_TEXT, "setTypeface", "(%sI)V" % TYPEFACE,
     "(%s%sI)V" % (TEXT_VIEW, TYPEFACE), FONTS),
    (PAINT_CLASS, "setTypeface", "(%s)%s" % (TYPEFACE, TYPEFACE),
     "(%s%s)%s" % (PAINT_CLASS, TYPEFACE, TYPEFACE), FONTS),

    (TEXT_VIEW, "setText", "(%s)V" % CHAR_SEQUENCE,
     "(%s%s)V" % (TEXT_VIEW, CHAR_SEQUENCE), BADGE),
    (VIEW, "setOnTouchListener", "(Landroid/view/View$OnTouchListener;)V",
     "(Landroid/view/View;Landroid/view/View$OnTouchListener;)V", BADGE),
    (TUX_TEXT, "setText", "(%s)V" % CHAR_SEQUENCE,
     "(%s%s)V" % (TEXT_VIEW, CHAR_SEQUENCE), BADGE),
    (TUX_TEXT, "setText", "(%sLandroid/widget/TextView$BufferType;)V" % CHAR_SEQUENCE,
     "(%s%sLandroid/widget/TextView$BufferType;)V" % (TEXT_VIEW, CHAR_SEQUENCE), BADGE),
    (TEXT_VIEW, "setText", "(%sLandroid/widget/TextView$BufferType;)V" % CHAR_SEQUENCE,
     "(%s%sLandroid/widget/TextView$BufferType;)V" % (TEXT_VIEW, CHAR_SEQUENCE), BADGE),


    # Which conversations have a streak going. `StreakData` holds the answer
    # and every one of its fields is named plainly, but nothing in the app ever
    # reads those fields, so there is nothing to listen to there. What the app
    # does do, constantly, is ask its own streak service about a conversation --
    # and `IStreakService` is a real name with real signatures. So the mod
    # listens to the questions instead of the answers: every conversation the
    # app asks about is one the mod can then ask about itself.
    #
    # The three method names are this release's, not the app's forever. They
    # are the one version-shaped thing in the streak feature, they live here
    # rather than in the Java, and the build counts what each of them matched.
    (STREAK_SERVICE, ("LJIJJLI", "streakOf"), "(%sZ)%s" % (STRING, STREAK_DATA),
     "(%s%sZ)%s" % (STREAK_SERVICE, STRING, STREAK_DATA), STREAKS),
    (STREAK_SERVICE, ("LJJJI", "showsStreak"), "(%sZ)Z" % STRING,
     "(%s%sZ)Z" % (STREAK_SERVICE, STRING), STREAKS),
    (STREAK_SERVICE, ("J", "streakOf"), "(%sZ)%s" % (STRING, STREAK_DATA),
     "(%s%sZ)%s" % (STREAK_SERVICE, STRING, STREAK_DATA), STREAKS),
    (STREAK_SERVICE, ("a0", "hasStreak"), "(%s)Z" % STRING,
     "(%s%s)Z" % (STREAK_SERVICE, STRING), STREAKS),
    (STREAK_SERVICE, ("h0", "showsStreak"), "(%sZ)Z" % STRING,
     "(%s%sZ)Z" % (STREAK_SERVICE, STRING), STREAKS),
    # every other question the app asks about one conversation, because the
    # first three between them only ever turned up a single conversation and a
    # feature that only knows about one chat is no feature
    (STREAK_SERVICE, ("w", "streakCount"), "(%s)I" % STRING,
     "(%s%s)I" % (STREAK_SERVICE, STRING), STREAKS),
    (STREAK_SERVICE, ("X", "asksAbout"), "(%s)Z" % STRING,
     "(%s%s)Z" % (STREAK_SERVICE, STRING), STREAKS),
    (STREAK_SERVICE, ("Y", "asksAboutToo"), "(%s)Z" % STRING,
     "(%s%s)Z" % (STREAK_SERVICE, STRING), STREAKS),
    (STREAK_SERVICE, ("l0", "streakState"), "(%s)Ljava/lang/Integer;" % STRING,
     "(%s%s)Ljava/lang/Integer;" % (STREAK_SERVICE, STRING), STREAKS),
    (STREAK_SERVICE, ("O", "streakText"), "(%s)%s" % (STRING, STRING),
     "(%s%s)%s" % (STREAK_SERVICE, STRING, STRING), STREAKS),

    # a sound pulled for copyright: the video stays and these four mute it
    (MUSIC, "available", "()Z", "(%s)Z" % MUSIC, SOUND),
    (MUSIC, "getMusicStatus", "()I", "(%s)I" % MUSIC, SOUND),
    (MUSIC, "isMuteShare", "()Z", "(%s)Z" % MUSIC, SOUND),
    (MUSIC, "getMuteType", "()I", "(%s)I" % MUSIC, SOUND),
]

# Statics of TikTok's own, which is how the flags are actually read: there is
# no receiver, so the instruction keeps its shape and only the class it lands
# in changes -- and the mod calls the original back by the same static.
MODEL_STATICS: List[Tuple[str, str, str, str, str]] = [
    (SETTINGS_MANAGER, (name, "flag"), "(%s)%s" % (args, kind),
     "(%s)%s" % (args, kind), FLAGS)
    for name, args, kind in AB_READERS
]

# 47.2.41 no longer has the legacy 0QeW profile-date gates. Caption
# renderers get a date from the exact post they are binding, without changing
# descriptions read for sharing, storage or comment bodies.
DATES = "Lcat/narezany/margyt/Dates;"
ANCHORED_SOURCES.append(("LX/0HCj;", "Landroid/widget/ImageView;", "setImageDrawable",
    "(Landroid/graphics/drawable/Drawable;)V",
    "(Landroid/widget/ImageView;Landroid/graphics/drawable/Drawable;)V", "Lcat/narezany/margyt/Heart;"))
CLASS_ANCHORED_SOURCES.add("LX/0HCj;")
ANCHORED_SOURCES.append(("Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;",
    VIEW,"findViewById","(I)Landroid/view/View;","(Landroid/view/View;I)Landroid/view/View;",COMMENTS))
for method in ("setIconRes", "setTintColor"):
    ANCHORED_SOURCES.append(("LX/0HCj;", "Lcom/bytedance/tux/icon/TuxIconView;", method,
        "(I)V", "(Lcom/bytedance/tux/icon/TuxIconView;I)V", "Lcat/narezany/margyt/Heart;"))

DATE_CAPTIONS = (
    "Lcom/ss/android/ugc/aweme/feed/assem/desc/VideoDescVM;",
    "Lcom/ss/android/ugc/aweme/feed/assem/desc/VideoDescAssem;",
    "Lcom/ss/android/ugc/aweme/feed/assem/desc/LandscapeVideoDescVM;",
    "Lcom/ss/android/ugc/aweme/search/arch/v2/protocol/card/components/mediaplayer/components/desc/ui/SearchMediaTextDescAssem;",
)
for anchor in DATE_CAPTIONS:
    ANCHORED_SOURCES.append((anchor, AWEME, "getDesc", "()Ljava/lang/String;",
                             "(Ljava/lang/Object;)Ljava/lang/String;", DATES))
    CLASS_ANCHORED_SOURCES.add(anchor)

# The same verified readers can be invoked through the concrete implementation.
MODEL_SOURCES += [("Lcom/ss/android/ugc/aweme/im/streak/impl/StreakService;",name,original,replacement,target)
    for owner,name,original,replacement,target in list(MODEL_SOURCES)
    if owner==STREAK_SERVICE and isinstance(name,tuple) and name[0] in ("LJIJJLI","LJJJI")]
MODEL_SOURCES.append(("Landroid/app/Dialog;","show","()V","(Landroid/app/Dialog;)V","Lcat/narezany/margyt/DownloadChoice;"))

MODEL_SOURCES.append((VIEW,"setVisibility","(I)V","(Landroid/view/View;I)V",RAIL))

# and which subscription the system calls the default, which is -1 when there
# is no card at all -- code that asks usually gives up on the spot
MODEL_STATICS += [
    (SUBSCRIPTIONS, "getDefaultDataSubscriptionId", "()I", "()I", REGION),
    (SUBSCRIPTIONS, "getDefaultVoiceSubscriptionId", "()I", "()I", REGION),
    (SUBSCRIPTIONS, "getDefaultSmsSubscriptionId", "()I", "()I", REGION),
    (SUBSCRIPTIONS, "getActiveDataSubscriptionId", "()I", "()I", REGION),
]

# owner, field, its type, ours, the class ours lives in. A field rather than a
# getter, so the read is an instruction of a different shape and becomes two.
FIELD_SOURCES: List[Tuple[str, str, str, str, str]] = [
    (FEED_ITEM_LIST, "items", LIST, "rawItems", FEED),
    ("Lcom/ss/android/ugc/aweme/search/pages/result/topsearch/core/model/SearchMixFeedList;", "mItems", LIST, "searchItems", FEED),
    (VIDEO_CONTROL, "allowDownload", BOXED_BOOLEAN, "allowDownload", DOWNLOAD),
    # a slideshow is not a video and never goes near getDownloadAddr: its
    # images carry the stamp themselves, with the clean one beside them
    # every sticker the app touches, so the settings have something to offer as
    # the one to send. `currentImage()` reads like the right place and is not:
    # five call sites in the whole apk, none of them on the way to a screen.
    # This field is read in a couple of hundred, which is what drawing one
    # actually looks like.
    (STICKER_ITEM, "stickerBase", STICKER_BASE, "stickerBase", STREAKS),
    (PHOTO_IMAGE, "ownerWatermarkImage", URL_MODEL, "ownerWatermarkImage", DOWNLOAD),
    (PHOTO_IMAGE, "userWatermarkImage", URL_MODEL, "userWatermarkImage", DOWNLOAD),
]

# The pink TikTok is built around. Most of the places it is drawn hold it as a
# plain constant in the bytecode, so each of those becomes a call into the mod
# and the colour turns into something a person can change. The build counts
# what it found and stops if the answer is none: a colour picker that changes
# nothing is worse than no colour picker.
TIKTOK_PINK = 0xFFFE2C55

# Colours that arrive through the framework rather than as a constant. Same
# rewrite as the telephony calls: the receiver becomes the first argument.
#
# Reading a colour is only half of it. One that was never read -- computed,
# blended, carried in from somewhere the mod cannot see -- still has to be
# applied to something before it reaches the screen, and the places it can be
# applied are few and have real names. Those are the second half of the list,
# and they are what reaches the parts a resource table never could.
COLOUR_SOURCES: List[Tuple[str, str, str]] = [
    ("Landroid/content/res/Resources;", "getColor",
     "(I)I", "(Landroid/content/res/Resources;I)I"),
    ("Landroid/content/res/Resources;", "getColor",
     "(ILandroid/content/res/Resources$Theme;)I",
     "(Landroid/content/res/Resources;ILandroid/content/res/Resources$Theme;)I"),
    ("Landroid/content/res/TypedArray;", "getColor",
     "(II)I", "(Landroid/content/res/TypedArray;II)I"),
    ("Landroid/content/Context;", "getColor",
     "(I)I", "(Landroid/content/Context;I)I"),

    # A background written in a layout is not a colour by the time the app sees
    # it -- the framework has already wrapped it in a ColorDrawable, and every
    # rule above looks straight past it. This is the way most of TikTok's
    # screens get their background, which is why the comments, the inbox and a
    # conversation kept TikTok's own while everything else moved.
    ("Landroid/content/res/TypedArray;", "getDrawable",
     "(I)Landroid/graphics/drawable/Drawable;",
     "(Landroid/content/res/TypedArray;I)Landroid/graphics/drawable/Drawable;"),
    ("Landroid/view/View;", "setBackgroundResource",
     "(I)V", "(Landroid/view/View;I)V"),

    # where a picture is asked for by number, which is where a texture pack
    # gets to answer instead
    ("Landroid/content/Context;", "getDrawable",
     "(I)Landroid/graphics/drawable/Drawable;",
     "(Landroid/content/Context;I)Landroid/graphics/drawable/Drawable;"),
    ("Landroid/content/res/Resources;", "getDrawable",
     "(I)Landroid/graphics/drawable/Drawable;",
     "(Landroid/content/res/Resources;I)Landroid/graphics/drawable/Drawable;"),
    ("Landroid/content/res/Resources;", "getDrawable",
     "(ILandroid/content/res/Resources$Theme;)Landroid/graphics/drawable/Drawable;",
     "(Landroid/content/res/Resources;ILandroid/content/res/Resources$Theme;)"
     "Landroid/graphics/drawable/Drawable;"),
    ("Landroid/widget/ImageView;", "setImageResource",
     "(I)V", "(Landroid/widget/ImageView;I)V"),
    ("Landroid/widget/ImageView;", "setImageTintList",
     "(Landroid/content/res/ColorStateList;)V",
     "(Landroid/widget/ImageView;Landroid/content/res/ColorStateList;)V"),

    # A Lottie animation -- the heart, the loading spinners -- is a json file
    # rather than a picture, read as a stream. Which makes it the one thing in
    # a texture pack somebody can edit in a text editor.
    ("Landroid/content/res/Resources;", "openRawResource",
     "(I)Ljava/io/InputStream;",
     "(Landroid/content/res/Resources;I)Ljava/io/InputStream;"),
    ("Landroid/content/res/Resources;", "openRawResource",
     "(ILandroid/util/TypedValue;)Ljava/io/InputStream;",
     "(Landroid/content/res/Resources;ILandroid/util/TypedValue;)Ljava/io/InputStream;"),
    ("Landroid/content/res/AssetManager;", "open",
     "(Ljava/lang/String;)Ljava/io/InputStream;",
     "(Landroid/content/res/AssetManager;Ljava/lang/String;)Ljava/io/InputStream;"),
    ("Landroid/content/res/AssetManager;", "open",
     "(Ljava/lang/String;I)Ljava/io/InputStream;",
     "(Landroid/content/res/AssetManager;Ljava/lang/String;I)Ljava/io/InputStream;"),

    # how far down a view is turned. TikTok fades its own overlay in and out
    # all the time -- when a video pauses, when a panel opens -- and every one
    # of those put back the brightness the mod had taken off
    ("Landroid/view/View;", "setAlpha", "(F)V", "(Landroid/view/View;F)V"),

    # a colour that comes with a state to go with it: enabled, pressed, chosen
    ("Landroid/content/res/Resources;", "getColorStateList",
     "(I)Landroid/content/res/ColorStateList;",
     "(Landroid/content/res/Resources;I)Landroid/content/res/ColorStateList;"),
    ("Landroid/content/res/TypedArray;", "getColorStateList",
     "(I)Landroid/content/res/ColorStateList;",
     "(Landroid/content/res/TypedArray;I)Landroid/content/res/ColorStateList;"),

    # where a colour is put to use: the brush, the shape, the tint, the text
    ("Landroid/view/Window;", "setStatusBarColor",
     "(I)V", "(Landroid/view/Window;I)V"),
    ("Landroid/view/Window;", "setNavigationBarColor",
     "(I)V", "(Landroid/view/Window;I)V"),
    ("Landroid/widget/TextView;", "setHintTextColor",
     "(I)V", "(Landroid/widget/TextView;I)V"),
    ("Landroid/graphics/drawable/Drawable;", "setTint",
     "(I)V", "(Landroid/graphics/drawable/Drawable;I)V"),
    ("Landroid/graphics/drawable/GradientDrawable;", "setColors",
     "([I)V", "(Landroid/graphics/drawable/GradientDrawable;[I)V"),
    ("Landroid/graphics/Paint;", "setColor",
     "(I)V", "(Landroid/graphics/Paint;I)V"),
    ("Landroid/text/TextPaint;", "setColor",
     "(I)V", "(Landroid/graphics/Paint;I)V"),
    ("Landroid/graphics/drawable/GradientDrawable;", "setColor",
     "(I)V", "(Landroid/graphics/drawable/GradientDrawable;I)V"),
    ("Landroid/widget/ImageView;", "setColorFilter",
     "(I)V", "(Landroid/widget/ImageView;I)V"),
    ("Landroid/widget/ImageView;", "setColorFilter",
     "(ILandroid/graphics/PorterDuff$Mode;)V",
     "(Landroid/widget/ImageView;ILandroid/graphics/PorterDuff$Mode;)V"),
    ("Landroid/widget/TextView;", "setTextColor",
     "(I)V", "(Landroid/widget/TextView;I)V"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setTextColor",
     "(I)V", "(Landroid/widget/TextView;I)V"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setHintTextColor",
     "(I)V", "(Landroid/widget/TextView;I)V"),
    ("Landroid/view/View;", "setBackgroundColor",
     "(I)V", "(Landroid/view/View;I)V"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setTextColorRes", "(I)V", "(Landroid/widget/TextView;I)V"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setTextColor", "(I)V", "(Landroid/widget/TextView;I)V"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setTextColor", "(Landroid/content/res/ColorStateList;)V", "(Landroid/widget/TextView;Landroid/content/res/ColorStateList;)V"),
    ("Lcom/bytedance/tux/icon/TuxIconView;", "setTintColorRes", "(I)V", "(Lcom/bytedance/tux/icon/TuxIconView;I)V"),
    ("Lcom/bytedance/tux/icon/TuxIconView;", "setTintColor", "(I)V", "(Lcom/bytedance/tux/icon/TuxIconView;I)V"),
    # TikTok's own icon view, and a real name at that
    ("Lcom/bytedance/tux/icon/TuxIconView;", "setColor",
     "(I)V", "(Lcom/bytedance/tux/icon/TuxIconView;I)V"),
]

# Typed colour-state setters and outlines previously bypassed the accent.
for _text_owner in (TEXT_VIEW,TUX_TEXT):
    COLOUR_SOURCES.append((_text_owner,"setText","(Ljava/lang/CharSequence;)V",
                           "(Landroid/widget/TextView;Ljava/lang/CharSequence;)V"))
for _owner, _name, _descriptor, _receiver in (
    ("Landroid/graphics/drawable/Drawable;", "setTintList", "(Landroid/content/res/ColorStateList;)V", "Landroid/graphics/drawable/Drawable;"),
    ("Landroid/view/View;", "setBackgroundTintList", "(Landroid/content/res/ColorStateList;)V", "Landroid/view/View;"),
    ("Landroid/graphics/drawable/GradientDrawable;", "setColor", "(Landroid/content/res/ColorStateList;)V", "Landroid/graphics/drawable/GradientDrawable;"),
    ("Landroid/graphics/drawable/GradientDrawable;", "setStroke", "(II)V", "Landroid/graphics/drawable/GradientDrawable;"),
    ("Landroid/graphics/drawable/GradientDrawable;", "setStroke", "(ILandroid/content/res/ColorStateList;)V", "Landroid/graphics/drawable/GradientDrawable;"),
    ("Landroid/widget/TextView;", "setTextColor", "(Landroid/content/res/ColorStateList;)V", "Landroid/widget/TextView;"),
    ("Landroid/widget/TextView;", "setHintTextColor", "(Landroid/content/res/ColorStateList;)V", "Landroid/widget/TextView;"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setTextColor", "(Landroid/content/res/ColorStateList;)V", "Landroid/widget/TextView;"),
    ("Lcom/bytedance/tux/input/TuxTextView;", "setHintTextColor", "(Landroid/content/res/ColorStateList;)V", "Landroid/widget/TextView;"),
):
    COLOUR_SOURCES.append((_owner,_name,_descriptor,"("+_receiver+_descriptor[1:]))

# A static of the framework's own: no receiver, so the call keeps its shape
# exactly and only the class it lands in changes.
COLOUR_STATICS: List[Tuple[str, str, str]] = [
    ("Landroid/content/res/ColorStateList;", "valueOf",
     "(I)Landroid/content/res/ColorStateList;"),
]

# method name -> (descriptor as TikTok calls it, descriptor of the static that
# replaces it -- the same, with the receiver moved into the arguments)
TARGETS: List[Tuple[str, str, str]] = [
    ("getSimCountryIso", "()Ljava/lang/String;", "(%s)Ljava/lang/String;" % TELEPHONY),
    ("getNetworkCountryIso", "()Ljava/lang/String;", "(%s)Ljava/lang/String;" % TELEPHONY),
    ("getSimOperator", "()Ljava/lang/String;", "(%s)Ljava/lang/String;" % TELEPHONY),
    ("getNetworkOperator", "()Ljava/lang/String;", "(%s)Ljava/lang/String;" % TELEPHONY),
    ("getSimOperatorName", "()Ljava/lang/String;", "(%s)Ljava/lang/String;" % TELEPHONY),
    ("getNetworkOperatorName", "()Ljava/lang/String;", "(%s)Ljava/lang/String;" % TELEPHONY),
    ("getSimState", "()I", "(%s)I" % TELEPHONY),
    ("getSimState", "(I)I", "(%sI)I" % TELEPHONY),
    ("hasIccCard", "()Z", "(%s)Z" % TELEPHONY),
    ("isNetworkRoaming", "()Z", "(%s)Z" % TELEPHONY),
    ("getSimCarrierId", "()I", "(%s)I" % TELEPHONY),
]


# Methods that have to answer no, whatever they would have worked out.
#
# TikTok signs in with Google two ways: through Play Services, and through the
# browser with AppAuth and a custom-scheme redirect. It asks the first one
# whether it is available and only falls back to the second when it is not.
#
# For anything built here the Play Services way cannot work at all: Google
# checks the package name against the certificate's SHA-1, and the certificate
# is no longer TikTok's. Left alone it fails with a developer error and no way
# forward. So the provider that speaks to Play Services reports itself
# unavailable, and the app takes its own fallback -- the browser, which checks
# nothing but who receives the redirect.
#
# The class name is a real one, not an obfuscated one, which is what makes this
# safe to anchor on.
FORCED_FALSE: List[Tuple[str, str]] = [
    ("com/bytedance/lobby/google/GoogleAuth", "isAvailable()Z"),
]


def rules() -> List[Tuple[str, "re.Pattern[str]", str]]:
    out = []
    for name, original, replacement in TARGETS:
        pattern = re.compile(
            r"invoke-virtual(/range)? (\{[^}]*\}), %s->%s%s"
            % (re.escape(TELEPHONY), name, re.escape(original))
        )
        target = r"invoke-static\1 \2, %s->%s%s" % (REGION, name, replacement)
        out.append((name + original, pattern, target))
    return out


def accent_rules() -> List[Tuple[str, "re.Pattern[str]", str]]:
    """The pink, wherever the bytecode spells it out or asks for it."""
    out = []
    literal = "-0x%x" % ((1 << 32) - TIKTOK_PINK) if TIKTOK_PINK > 0x7FFFFFFF \
        else "0x%x" % TIKTOK_PINK
    # const vX, -0x1d3ab  ->  a call, and the answer in the same register
    out.append((
        "the pink itself",
        re.compile(r"^(\s*)const ([vp]\d+), %s$" % re.escape(literal), re.MULTILINE),
        r"\1invoke-static {}, %s->accent()I\n\n\1move-result \2" % ACCENT,
    ))
    for owner, name, original, replacement in COLOUR_SOURCES:
        out.append((
            "%s->%s%s" % (owner.split("/")[-1][:-1], name, original),
            re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), name, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (ACCENT, name, replacement),
        ))
    for owner, name, signature in COLOUR_STATICS:
        out.append((
            "%s->%s" % (owner.split("/")[-1][:-1], name),
            re.compile(r"invoke-static(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), name, re.escape(signature))),
            r"invoke-static\1 \2, %s->%s%s" % (ACCENT, name, signature),
        ))
    # TikTok 47.2.41 does not open these animations through the framework
    # AssetManager call sites above. The feed's DiggAnimationView hands the
    # filename directly to its Lottie view, which then parses the asset on a
    # background executor. Route that one call through Accent so its JSON is
    # recoloured before the composition is cached. The marker strings below
    # make sure only the dex that owns the like-animation code is disassembled.
    out.append((
        "feed Lottie setAnimation",
        re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), "
                   r"LX/1HkT;->setAnimation\(Ljava/lang/String;\)V"),
        r"invoke-static\1 \2, %s->setAnimation(Ljava/lang/Object;Ljava/lang/String;)V"
        % ACCENT,
    ))
    return out


# Resource text, hints and native text styles must also apply the chosen font.
for _font_owner in (TEXT_VIEW,TUX_TEXT,"Landroid/widget/EditText;","Landroid/widget/Button;"):
    for _font_name,_font_sig in (
        ("setText","(I)V"),("setHint","(I)V"),("setHint","(Ljava/lang/CharSequence;)V"),
        ("setTextAppearance","(I)V"),("setTextAppearance","(Landroid/content/Context;I)V"),
    ):
        MODEL_SOURCES.append((_font_owner,_font_name,_font_sig,"("+TEXT_VIEW+_font_sig[1:],FONTS))


MODEL_SOURCES += [
    (FRIENDS_RESPONSE,"getUserList","()Ljava/util/List;","(Ljava/lang/Object;)Ljava/util/List;","Lcat/narezany/margyt/PinnedFriends;"),
    ("Landroid/app/NotificationManager;","notify","(ILandroid/app/Notification;)V","(Landroid/app/NotificationManager;ILandroid/app/Notification;)V","Lcat/narezany/margyt/ModNotifications;"),
    ("Landroid/app/NotificationManager;","notify","(Ljava/lang/String;ILandroid/app/Notification;)V","(Landroid/app/NotificationManager;Ljava/lang/String;ILandroid/app/Notification;)V","Lcat/narezany/margyt/ModNotifications;"),
]

def model_rules() -> List[Tuple[str, "re.Pattern[str]", str]]:
    """Calls on TikTok's own models, answered by the mod instead."""
    out = []
    for owner, name, original, replacement, target in MODEL_SOURCES:
        # a pair when what the mod calls it differs from what TikTok does:
        # the app's obfuscated name on the way in, a readable one on the way out
        theirs, ours = name if isinstance(name, tuple) else (name, name)
        out.append((
            "%s->%s" % (owner.rsplit("/", 1)[-1][:-1], theirs),
            # an interface call is the same 35c instruction under another
            # mnemonic, and a service reached through one is still a receiver
            re.compile(r"invoke-(?:virtual|interface)(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), theirs, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, ours, replacement),
        ))
    for owner, name, original, replacement, target in MODEL_STATICS:
        theirs, ours = name if isinstance(name, tuple) else (name, name)
        out.append((
            "%s->%s" % (owner.rsplit("/", 1)[-1][:-1], theirs),
            re.compile(r"invoke-static(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), theirs, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, ours, replacement),
        ))
    for label, descriptor, ours, target in DISCOVERED_VIRTUALS:
        found = FOUND.get(label)
        if found is None:
            continue
        owner, theirs = found
        # the receiver is already the first register of an invoke-virtual, so
        # the instruction keeps its registers and only its kind changes; the
        # mod takes that receiver as a plain Object and hands the call back
        out.append((
            label,
            re.compile(r"invoke-virtual(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), re.escape(theirs), re.escape(descriptor))),
            r"invoke-static\1 \2, %s->%s%s"
            % (target, ours, "(Ljava/lang/Object;" + descriptor[1:]),
        ))
    for label, descriptor, ours, target in DISCOVERED_STATICS:
        found = FOUND.get(label)
        if found is None:
            continue
        owner, theirs = found
        out.append((
            label,
            re.compile(r"invoke-static(/range)? (\{[^}]*\}), %s->%s%s"
                       % (re.escape(owner), re.escape(theirs), re.escape(descriptor))),
            r"invoke-static\1 \2, %s->%s%s" % (target, ours, descriptor),
        ))
    for name, original, replacement, target in WILD_SOURCES:
        out.append((
            "%s (any owner)" % name,
            re.compile(r"invoke-(?:virtual|interface)(/range)? (\{[^}]*\}), L[^;]+;->%s%s"
                       % (name, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, name, replacement),
        ))
    for owner, field, kind, name, target in FIELD_SOURCES:
        # iget-object vA, vB, Owner->field:Type
        #   -> invoke-static {vB}, Ours->name(Owner)Type ; move-result-object vA
        #
        # Which iget it is depends on what is being read: a long or a double is
        # two registers wide and has instructions of its own, and reading one
        # with the wrong instruction is not something the assembler forgives.
        if kind in ("J", "D"):
            read, took = "iget-wide", "move-result-wide"
        elif kind in ("Z", "B", "S", "C", "I", "F"):
            read, took = "iget", "move-result"
        else:
            read, took = "iget-object", "move-result-object"
        out.append((
            "%s.%s" % (owner.rsplit("/", 1)[-1][:-1], field),
            re.compile(r"^(\s*)%s ([vp]\d+), ([vp]\d+), %s->%s:%s$"
                       % (read, re.escape(owner), re.escape(field), re.escape(kind)),
                       re.MULTILINE),
            r"\1invoke-static {\3}, %s->%s(%s)%s\n\n\1%s \2"
            % (target, name, owner, kind, took),
        ))
    return out


def rewrite_models(root: str) -> Dict[str, int]:
    """Rewrite every call on TikTok's models, counting them by signature."""
    counts: Dict[str, int] = {}
    prepared = model_rules()
    owners = tuple(set([owner for owner, _n, _o, _r, _t in MODEL_SOURCES]
                       + [owner for owner, _n, _o, _r, _t in MODEL_STATICS]
                       + [owner for owner, _f, _k, _n, _t in FIELD_SOURCES]
                       # a rule with no owner of its own is recognised by the
                       # method it is looking for, or its file is never opened
                       + [name for name, _o, _r, _t in WILD_SOURCES]
                       # and one whose owner was found rather than written down
                       # is recognised by the owner that was found
                       + [FOUND[label][0]
                          for label, _d, _o, _t in DISCOVERED_STATICS + DISCOVERED_VIRTUALS
                          if label in FOUND]))
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if not any(owner in text for owner in owners):
                continue
            before = text
            for label, pattern, target in prepared:
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts


# This native helper exclusively formats comment clipboard payloads, including
# the normal Copy action and saved comments. Match callers, not generic clips.
ANCHORED_SOURCES.append(("LX/0oir;","LX/0oir;","LIZ",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)Landroid/content/ClipData;",
    "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)Landroid/content/ClipData;",
    "Lcat/narezany/margyt/CommentClipboard;"))

def anchored_rules() -> List[Tuple[str, str, "re.Pattern[str]", str]]:
    """Rules and the marker the class they belong in has to carry."""
    out = []
    for anchor, owner, name, original, replacement, target in ANCHORED_SOURCES:
        out.append((
            anchor,
            "%s->%s (in the class marked %s)" % (owner.rsplit("/", 1)[-1][:-1], name, anchor),
            re.compile(r"invoke-%s(/range)? (\{[^}]*\}), %s->%s%s"
                       % ("static" if owner=="LX/0oir;" else "virtual", re.escape(owner), name, re.escape(original))),
            r"invoke-static\1 \2, %s->%s%s" % (target, name, replacement),
        ))
    return out


TEXT_LIMITS = "Lcat/narezany/margyt/TextLimits;"
TEXT_LIMIT_MARKERS = (
    "LX/0onX;", "maxInputLimit", "LX/08uW;", "LX/0H5v;",
    "studio_publish_hashtag_max_limit", "studio_publish_limit_hashtag_switch",
    "Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;",
)


def text_limit_rules(text: str) -> Tuple[str, Dict[str, int]]:
    """47.2.41 limits at their native sources, preserving payloads and other filters."""
    counts: Dict[str, int] = {}
    if not any(marker in text for marker in TEXT_LIMIT_MARKERS):
        return text, counts
    declaration = text.split("\n", 1)[0]

    def bound(register: str, method: str, wide: bool = False) -> str:
        end = register[0] + str(int(register[1:]) + 1) if wide else register
        kind = "J" if wide else "I"
        return ("\n    invoke-static/range {" + register + " .. " + end + "}, "
                + TEXT_LIMITS + "->" + method + "(" + kind + ")" + kind
                + "\n    move-result" + ("-wide" if wide else "") + " " + register + "\n")

    def method_rule(match):
        body = match.group(0)
        header = body.split("\n", 1)[0]
        # Each native method is patched once, including .line-rich production smali.
        if TEXT_LIMITS + "->" in body:
            return body
        helper = label = None
        if "LX/0onX;" in declaration and " LIZIZ()I" in header:
            helper, label = "comment", "native comment limit"
        elif "Lw3j/e;" in declaration:
            if " LIZ()I" in header and "studio_publish_hashtag_max_limit" in body:
                helper, label = "hashtags", "native hashtag maximum"
            elif " LIZJ()Z" in header and "studio_publish_limit_hashtag_switch" in body:
                helper, label = "hashtagGate", "native hashtag gate"
        if helper:
            def returned(found):
                register = found.group(1)
                if helper == "hashtagGate":
                    hook = ("\n    invoke-static/range {" + register + " .. " + register + "}, "
                            + TEXT_LIMITS + "->hashtagGate(Z)Z\n    move-result " + register + "\n")
                else:
                    hook = bound(register, helper)
                return hook + found.group(0)
            body, hits = re.subn(r"(?m)^\s*return ([vp]\d+)$", returned, body)
            counts[label] = counts.get(label, 0) + hits
        # Long-comment writing, mention insertion and send-button validation.
        def long_limit(found):
            return found.group(0) + bound(found.group(1), "commentLong", True)
        body, hits = re.subn(
            r"(?m)^\s*iget-wide ([vp]\d+), [vp]\d+, Lcom/ss/android/ugc/aweme/comment/experiment/LongCommentWritingConfig;->maxInputLimit:J$",
            long_limit, body)
        if hits:
            counts["native long comment limit"] = counts.get("native long comment limit", 0) + hits
        # Read the original lazy value first, then override it. Cached AB values
        # cannot freeze the mod's switch for the remainder of the app session.
        debug = r"\s+(?:\.line[^\n]*\n\s*)*"
        repost_read = (r"sget-object ([vp]\d+), LX/08uW;->LIZ:LX/01SE;" + debug
                       + r"invoke-interface \{\1\}, LX/01SE;->getValue\(\)Ljava/lang/Object;" + debug
                       + r"move-result-object ([vp]\d+)" + debug
                       + r"check-cast \2, Ljava/lang/Number;" + debug
                       + r"invoke-virtual \{\2\}, Ljava/lang/Number;->intValue\(\)I" + debug
                       + r"move-result ([vp]\d+)")
        body, hits = re.subn(repost_read, lambda m: m.group(0) + bound(m.group(3), "repost"), body)
        if hits:
            counts["native repost limit"] = counts.get("native repost limit", 0) + hits
        # Older feed-note input: one Unicode-aware filter has a constructor
        # bound, the other has its 30-character maximum in its own arithmetic.
        if "Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;" in declaration:
            pattern = r"(?m)^\s*invoke-direct \{[vp]\d+, ([vp]\d+), [vp]\d+\}, LX/0HvS;-><init>\(ILkotlin/jvm/functions/Function0;\)V$"
            body, hits = re.subn(pattern, lambda m: bound(m.group(1), "repost") + m.group(0), body)
            if hits:
                counts["native repost note limit"] = counts.get("native repost note limit", 0) + hits
        if "LX/0H5v;" in declaration and " filter(" in header:
            def remaining(m):
                dst, used = m.group(1), m.group(2)
                return ("\n    const/16 " + dst + ", 0x1e" + bound(dst, "repost")
                        + "    sub-int/2addr " + dst + ", " + used)
            body, hits = re.subn(r"(?m)^\s*rsub-int/lit8 ([vp]\d+), ([vp]\d+), 0x1e$", remaining, body)
            if hits:
                counts["native repost note fallback"] = counts.get("native repost note fallback", 0) + hits
        return body

    text = re.sub(r"(?ms)^\.method[^\n]*\n.*?^\.end method", method_rule, text)
    return text, counts


def rewrite_anchored(root: str) -> Dict[str, int]:
    """Rewrite calls inside the one class that carries the marker."""
    counts: Dict[str, int] = {}
    prepared = anchored_rules()
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            before = text
            declaration=text.split("\n",1)[0]
            text, limit_counts = text_limit_rules(text)
            for label, hits in limit_counts.items():
                counts[label] = counts.get(label, 0) + hits
            if "->draftProgressBar:I" in text or "can not show seekbar, state: 4" in text:
                def seekbar_policy(match):
                    body = match.group(0)
                    if SEEKBAR + "->allow(Z)Z" in body:
                        return body
                    # The native drag predicate first excludes live/photo/ad cards.
                    # Change only its experiment result, after those checks.
                    if all(marker in body for marker in ("->draftProgressBar:I", "->isLive(", "->isPhotoMode(")):
                        pattern = r"invoke-virtual \{[vp]\d+\}, Ljava/lang/Boolean;->booleanValue\(\)Z\s+(?:\.line[^\n]*\n\s*)*move-result ([vp]\d+)"
                        label, limit = "native seekbar eligibility", 1
                    elif "can not show seekbar, state: 4" in body and "can not show seekbar, state: 6" in body:
                        # Preserve the controller's resumed/current-item/content guards.
                        pattern = (r"invoke-static \{[vp]\d+\}, (?:LX/1Cda;->LIZJ|LX/08iI;->LIZ)"
                                   + re.escape("(" + AWEME + ")Z") + r"\s+(?:\.line[^\n]*\n\s*)*move-result ([vp]\d+)")
                        label, limit = "native seekbar progress policy", 0
                    else:
                        return body
                    def allow(found):
                        register = found.group(1)
                        return (found.group(0) + "\n\n    invoke-static/range {" + register + " .. " + register
                                + "}, " + SEEKBAR + "->allow(Z)Z\n\n    move-result " + register)
                    body, hits = re.subn(pattern, allow, body, count=limit)
                    if hits:
                        counts[label] = counts.get(label, 0) + hits
                    return body
                text = re.sub(r"(?ms)^\.method .*?^\.end method", seekbar_policy, text)
            if FEED_ITEM_LIST in declaration:
                def native_ad_getter(match):
                    body=match.group(0)
                    if "Feed;->withoutAds" in body:return body
                    body,hits=re.subn(r"(?m)^    return-object ([vp]\d+)",lambda m:"    invoke-static/range {"+m.group(1)+" .. "+m.group(1)+"}, "+FEED+"->withoutAds(Ljava/util/List;)Ljava/util/List;\n    move-result-object "+m.group(1)+"\n"+m.group(0),body)
                    if hits:counts["native feed ad getter"]=hits
                    return body
                text=re.sub(r"(?ms)^\.method public getItems\(\)Ljava/util/List;.*?^\.end method",native_ad_getter,text)
                def native_ad_setter(match):
                    body=match.group(0)
                    if "Feed;->withoutAds" in body:return body
                    hook="\n    invoke-static/range {p1 .. p1}, "+FEED+"->withoutAds(Ljava/util/List;)Ljava/util/List;\n    move-result-object p1\n"
                    body,hits=re.subn(r"(?m)^    \.registers [0-9]+",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native feed ad ingress"]=hits
                    return body
                text=re.sub(r"(?ms)^\.method public setItems\(Ljava/util/List;\)V.*?^\.end method",native_ad_setter,text)
            if "Lcom/ss/android/ugc/aweme/comment/longpresspanel/ui/CommentLongPressFragment;" in declaration:
                def native_comment_items(match):
                    body=match.group(0)
                    if "Comments;->nativeItems" in body:return body
                    hook="\n    invoke-static {p0, p1}, "+COMMENTS+"->nativeItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;\n    move-result-object p1\n"
                    body,hits=re.subn(r"(?m)^    \.registers [0-9]+",lambda m:m.group(0)+hook,body,count=1)
                    if hits:counts["native comment selection item"]=hits
                    return body
                text=re.sub(r"(?ms)^\.method public final oX\(Ljava/util/List;\)V.*?^\.end method",native_comment_items,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;" in declaration:
                def offline_legacy(match):
                    body=match.group(0)
                    hook="    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/OfflineChoices;->legacy(Ljava/lang/Object;)V\n"
                    if hook.strip() not in body:
                        body=re.sub(r"(?m)^    return-void",hook+"    return-void",body)
                        counts["native offline legacy menu"]=counts.get("native offline legacy menu",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final onAssemPostCreate\(\)V.*?^\.end method",offline_legacy,text)
            comment_owner = next((owner for owner in (
                "Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;",
                "LX/0oah;",
                "Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2;",
                "Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2SplitJson;"
            ) if owner in declaration), None)
            if comment_owner:
                def offline_native_response(match):
                    body=match.group(0)
                    if "OfflineComments;->native" in body:return body
                    task=comment_owner.endswith("/CommentApi;")
                    static=task or comment_owner=="LX/0oah;"
                    register="p0" if static else "p1"
                    method="nativeTask" if task else "nativeObservable"
                    result="LX/0Vgo;" if task else "LX/19ZK;"
                    hook=(f"    invoke-static/range {{{register} .. {register}}}, Lcat/narezany/margyt/OfflineComments;->{method}(Ljava/lang/String;)Ljava/lang/Object;\n"
                          "    move-result-object v0\n    if-eqz v0, :ttcuz_comments_online\n"
                          f"    check-cast v0, {result}\n    return-object v0\n    :ttcuz_comments_online\n")
                    # Preserve method/parameter annotations; insert at the first instruction.
                    body,hits=re.subn(r"(?m)^(    (?:move|const|invoke|sget|iget|new|return|if|check)[\w/-]*(?: |$))",lambda m:hook+m.group(0),body,count=1)
                    if hits:counts["native offline comments response"]=counts.get("native offline comments response",0)+1
                    return body
                signature=r"(?ms)^\.method public static LIZIZ\(.*?^\.end method" if comment_owner in ("LX/0oah;","Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;") else r"(?ms)^\.method public fetchCommentListV2\(.*?^\.end method"
                text=re.sub(signature,offline_native_response,text)
            if '"streak_icon_inline_"' in text:
                def native_streak_art(match):
                    body=match.group(0)
                    if "Accent;->keepNativeArtwork" in body:return body
                    def protect(result):
                        register=result.group(1)
                        counts["native streak artwork"]=counts.get("native streak artwork",0)+1
                        return (f"    invoke-static/range {{{register} .. {register}}}, Lcat/narezany/margyt/Accent;->keepNativeArtwork(Landroid/graphics/drawable/Drawable;)Landroid/graphics/drawable/Drawable;\n"
                                f"    move-result-object {register}\n    return-object {register}")
                    return re.sub(r"(?m)^    return-object ([vp]\d+)$",protect,body)
                text=re.sub(r"(?ms)^\.method [^\n]+\)Landroid/graphics/drawable/Drawable;.*?^\.end method",native_streak_art,text)
            if "LX/0wk3;" in declaration:
                def offline_saved(match):
                    body=match.group(0)
                    if "OfflineComments;->cached" not in body:
                        body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/OfflineComments;->cached(Ljava/lang/Object;Ljava/lang/String;)V",body,count=1)
                        counts["native offline comments cache"]=counts.get("native offline comments cache",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public static LIZ\(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;\)V.*?^\.end method",offline_saved,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/ui/OfflineModeContentComponent;" in declaration:
                def offline_content(match):
                    body=match.group(0)
                    if "OfflineComments;->content" not in body:
                        body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/OfflineComments;->content(Ljava/lang/Object;)V",body,count=1)
                        counts["native offline comments current video"]=counts.get("native offline comments current video",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final onParentViewCreated\(\)V.*?^\.end method",offline_content,text)
                def offline_closed(match):
                    body=match.group(0)
                    if "OfflineComments;->closed" not in body:
                        body=re.sub(r"(?m)^    return-void","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/OfflineComments;->closed(Ljava/lang/Object;)V\n    return-void",body)
                    return body
                text=re.sub(r"(?ms)^\.method public final onDestroy\(\)V.*?^\.end method",offline_closed,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/ui/bottom/OfflineModeDetailBottomComponent;" in declaration:
                def offline_bottom(match):
                    body=match.group(0)
                    if "OfflineComments;->bottom" not in body and "    return-object v4" in body:
                        body=body.replace("    return-object v4","    invoke-static {v4}, Lcat/narezany/margyt/OfflineComments;->bottom(Landroid/view/View;)Landroid/view/View;\n    move-result-object v4\n    return-object v4")
                        counts["native offline comments viewer"]=counts.get("native offline comments viewer",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final MR2\(Landroid/view/ViewGroup;\)Landroid/view/View;.*?^\.end method",offline_bottom,text)
            if "Lcom/ss/android/ugc/aweme/offlinemode/viewmodel/OfflineModeManagerVM;" in declaration:
                def offline_valid(match):
                    body=match.group(0)
                    needle="invoke-interface {v1, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z"
                    if needle in body:
                        body=body.replace(needle,"invoke-static {v1, v0}, Lcat/narezany/margyt/OfflineChoices;->validCount(Ljava/util/List;Ljava/lang/Object;)Z")
                        counts["native offline custom persistence"]=counts.get("native offline custom persistence",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final d43\(\)Z.*?^\.end method",offline_valid,text)
            if "LX/0wjq;" in declaration:
                def offline_native_count(match):
                    body=match.group(0)
                    needle="invoke-interface {v1, v0}, Ljava/util/List;->contains(Ljava/lang/Object;)Z"
                    if needle in body:
                        body=body.replace(needle,"invoke-static {v1, v0}, Lcat/narezany/margyt/OfflineChoices;->validCount(Ljava/util/List;Ljava/lang/Object;)Z",1)
                        counts["native offline count accepted"]=counts.get("native offline count accepted",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public static LIZ\(\)I.*?^\.end method",offline_native_count,text)
            if "Lkotlin/jvm/internal/AwS704S0100000_26;" in declaration:
                def offline_list(match):
                    body=match.group(0)
                    hook="\n    invoke-static {v1}, Lcat/narezany/margyt/OfflineChoices;->counts(Ljava/util/List;)Ljava/util/List;\n    move-result-object v1"
                    needle=re.search(r"invoke-static \{\}, LX/0wj1;->LJFF\(\)Ljava/util/List;(?:(?:\s*\.line \d+)*)\s*move-result-object v1",body)
                    if "OfflineChoices;->counts" not in body and needle:
                        body=body[:needle.end()]+hook+body[needle.end():]
                        counts["native offline choices"]=counts.get("native offline choices",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public static final invoke\$3208\(Lkotlin/jvm/internal/AwS704S0100000_26;\)Ljava/lang/Object;.*?^\.end method",offline_list,text)
            if "LX/0wiL;" in declaration:
                def offline_row(match):
                    body=match.group(0)
                    if "OfflineChoices;->row" not in body:
                        body=body.replace("    return-object v10","    invoke-static {v3, v10}, Lcat/narezany/margyt/OfflineChoices;->row(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\n    move-result-object v10\n    check-cast v10, LX/0ldB;\n    return-object v10")
                        counts["native offline row"]=counts.get("native offline row",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final LIZLLL\(\)LX/0ldB;.*?^\.end method",offline_row,text)
            def add_exit(pattern,hook,label):
                nonlocal text
                def patch(match):
                    body=match.group(0)
                    if hook.strip() in body:return body
                    changed,hits=re.subn(r"(?m)^([ \t]*)return-void",lambda m:hook+m.group(0),body)
                    if hits:counts[label]=counts.get(label,0)+1
                    return changed
                text=re.sub(pattern,patch,text)
            if "Lcom/ss/android/ugc/aweme/feed/assem/share/VideoShareAssem;" in declaration:
                def capture_share(match):
                    body=match.group(0)
                    if "VideoActions;->captureShare" in body:return body
                    body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/VideoActions;->captureShare(Ljava/lang/Object;)V",body,count=1)
                    counts["native share capture"]=counts.get("native share capture",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final onClick\(Landroid/view/View;\)V.*?^\.end method",capture_share,text)
            if REPOST_CLASS in declaration:
                add_exit(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)|s4\(Ljava/lang/Object;\))V.*?^\.end method","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Repost;->bind(Ljava/lang/Object;)V\n","native repost")
            if "Lcom/bytedance/tux/icon/TuxIconView;" in declaration:
                add_exit(r"(?ms)^\.method public (?:constructor <init>\([^\n]*|final setTuxIcon\([^\n]*)\)V.*?^\.end method",
                         "    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Accent;->nativeIconBound(Lcom/bytedance/tux/icon/TuxIconView;)V\n",
                         "native icon bind contrast")
            if AVATAR_CLASS in declaration:
                add_exit(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)|s4\(Ljava/lang/Object;\))V.*?^\.end method","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Repost;->anchor(Ljava/lang/Object;)V\n","native repost avatar")
            if PROFILE_CONTAINER in declaration:
                add_exit(r"(?ms)^\.method public onViewCreated\(Landroid/view/View;\)V.*?^\.end method","    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/ProfileLayout;->bind(Ljava/lang/Object;Landroid/view/View;)V\n","native profile layout")
            if PROFILE_HEADER_CONTAINER in declaration:
                add_exit(r"(?ms)^\.method public onCreate\(\)V.*?^\.end method","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/ProfileLayout;->created(Ljava/lang/Object;)V\n","native profile header create")
            if any(owner in declaration for owner in MESSAGE_CLASSES):
                def message_bind(match):
                    body=match.group(0)
                    if "MessageTimes;->bind" in body:return body
                    body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+"\n    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/MessageTimes;->before(Ljava/lang/Object;)V",body,count=1)
                    body=re.sub(r"(?m)^    return-void","    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/MessageTimes;->bind(Ljava/lang/Object;Ljava/lang/Object;)V\n    return-void",body)
                    counts["native message timestamps"]=counts.get("native message timestamps",0)+1
                    return body
                message_pattern=(r"(?ms)^\.method public Bq\(LX/0Qfl;\)V.*?^\.end method" if MESSAGE_CLASSES[0] in declaration else r"(?ms)^\.method public final s4\(Ljava/lang/Object;\)V.*?^\.end method")
                text=re.sub(message_pattern,message_bind,text)
            if PUSH_GUIDE in declaration:
                def push_guide(match):
                    body=match.group(0)
                    if "ModNotifications;->noPrompts" in body:return body
                    hook="\n    invoke-static {}, Lcat/narezany/margyt/ModNotifications;->noPrompts()Z\n    move-result v0\n    if-eqz v0, :ttcuz_notification_guide\n    const/4 v0, 0x0\n    return v0\n    :ttcuz_notification_guide"
                    body=re.sub(r"(?m)^    \.registers \d+",lambda m:m.group(0)+hook,body,count=1)
                    counts["native notification guide"]=counts.get("native notification guide",0)+1
                    return body
                text=re.sub(r"(?ms)^\.method public final LIZ\(\)Z.*?^\.end method",push_guide,text)
            if PLAYER_CONTROLLER in declaration:
                def resumed_player(match):
                    body=match.group(0)
                    if "PlayerTools;->resumed" in body:return body
                    changed,hits=re.subn(r"(?m)^([ \t]*)return-void",lambda m:"    invoke-static/range {p0 .. p1}, Lcat/narezany/margyt/PlayerTools;->resumed(Ljava/lang/Object;Ljava/lang/String;)V\n"+m.group(0),body)
                    if hits:counts["native player resume"]=counts.get("native player resume",0)+1
                    return changed
                text=re.sub(r"(?ms)^\.method public final onResumePlay\(Ljava/lang/String;\)V.*?^\.end method",resumed_player,text)
            if any(owner in declaration for owner in RAIL_CLASSES):
                def rail_slot(match):
                    body=match.group(0)
                    if "FeedRail;->bindNative" in body:return body
                    hook="    invoke-static/range {p0 .. p0}, "+RAIL+"->bindNative(Ljava/lang/Object;)V\n"
                    changed,hits=re.subn(r"(?m)^([ \t]*)return-void",lambda m:hook+m.group(0),body)
                    if hits:counts["native right-area slots"]=counts.get("native right-area slots",0)+1
                    return changed
                text=re.sub(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)|s4\(Ljava/lang/Object;\))V.*?^\.end method",rail_slot,text)
            for anchor, label, pattern, target in prepared:
                if anchor not in text:
                    continue
                if anchor in CLASS_ANCHORED_SOURCES:
                    declaration = text.split("\n", 1)[0]
                    if anchor not in declaration:
                        continue
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if "LX/0dQx;" in text.split("\n",1)[0]:
                def repost_texture(match):
                    body=match.group(0)
                    if "Repost;->textureChanged" in body:return body
                    body,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Repost;->textureChanged(Landroid/view/View;)V\n"+r"\1",body)
                    if hits:counts["native repost texture state"]=counts.get("native repost texture state",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final (?:LJ|LJFF)\(\)V.*?^\.end method",repost_texture,text)
            if "LX/0oYe;" in text.split("\n",1)[0]:
                def selected_dislike(match):
                    body=match.group(0)
                    if "Accent;->dislike" in body:return body
                    body,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/Accent;->dislike(Ljava/lang/Object;)V\n"+r"\1",body)
                    if hits:counts["selected dislike accent"]=counts.get("selected dislike accent",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public final LJIIIZ\(Ljava/lang/Boolean;\)V.*?^\.end method",selected_dislike,text)
            if "Lcom/ss/android/ugc/aweme/commentv2/commentlist/powercell/BaseCommentCell;" in text.split("\n",1)[0]:
                def bind_comment_body(match):
                    body=match.group(0)
                    if "Comments;->bindCell" in body:return body
                    body,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, "+COMMENTS+"->bindCell(Ljava/lang/Object;)V\n"+r"\1",body)
                    if hits:counts["native comment body actions"]=counts.get("native comment body actions",0)+hits
                    return body
                text=re.sub(r"(?ms)^\.method public bridge synthetic onBindItemView\([^\n]*\n.*?^\.end method",bind_comment_body,text)
                def native_comment_actions(match):
                    method=match.group(0)
                    if "CommentActionMenuVM;->h43" not in method or "Comments;->openNativeActions" in method:return method
                    hook=("\n    invoke-static/range {p0 .. p0}, "+COMMENTS+"->openNativeActions(Ljava/lang/Object;)Z\n"
                          "    move-result v0\n    if-eqz v0, :ttcuz_native_comment_menu\n    return-void\n    :ttcuz_native_comment_menu\n")
                    result,hits=re.subn(r"(?m)^(    \.registers [0-9]+)",lambda m:m.group(0)+hook,method,count=1)
                    if hits:counts["native comment text actions"]=counts.get("native comment text actions",0)+hits
                    return result
                text=re.sub(r"(?ms)^\.method public k8\(\)V.*?^\.end method",native_comment_actions,text)
                pattern = (r"(?m)^([ \t]*iget-object ([vp]\d+), [vp]\d+, "
                    r"Landroidx/recyclerview/widget/RecyclerView\$ViewHolder;->itemView:Landroid/view/View;)"
                    r"(?!\n[ \t]*invoke-static/range \{\2 \.\. \2\}, Lcat/narezany/margyt/Comments;->mark)")
                text,hits=re.subn(pattern,lambda m:m.group(1)+"\n    invoke-static/range {"+m.group(2)+" .. "+m.group(2)+"}, "
                    +COMMENTS+"->mark(Landroid/view/View;)V",text)
                if hits:counts["native comment cell protection"]=counts.get("native comment cell protection",0)+hits
            if "Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoRelationAssem;" in text.split("\n",1)[0]:
                def author_date(match):
                    method=match.group(0)
                    if "Dates;->bindAuthor" in method:return method
                    changed,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, "+DATES+"->bindAuthor(Ljava/lang/Object;)V\n"+r"\1",method)
                    if hits:counts["native author date slot"]=counts.get("native author date slot",0)+hits
                    return changed
                text=re.sub(r"(?ms)^\.method public final (?:onViewCreated\(Landroid/view/View;\)V|s4\(Ljava/lang/Object;\)V).*?^\.end method",author_date,text)
            if "Lcom/ss/android/ugc/aweme/feed/assem/desc/VideoDescAssem;" in text.split("\n",1)[0]:
                def selected_date(match):
                    method=match.group(0)
                    if "onHolderSelected: awemeId=" not in method or "Dates;->bindPost" in method:return method
                    registers=re.search(r"\.registers (\d+)",method)
                    if not registers or int(registers.group(1))>16:return method
                    aid=re.search(r"invoke-virtual \{([vp]\d+)\}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid\(\)Ljava/lang/String;",method)
                    if not aid:return method
                    register=aid.group(1)
                    pattern=r"(?m)^([ \t]*if-eqz "+register+r", :[^\n]+)"
                    def bind(m):return "    invoke-static {p0, "+register+"}, "+DATES+"->bindPost(Ljava/lang/Object;Ljava/lang/Object;)V\n"+m.group(1)
                    changed,hits=re.subn(pattern,bind,method,count=1)
                    if hits:counts["visible publication date"]=counts.get("visible publication date",0)+hits
                    return changed
                text=re.sub(r"(?ms)^\.method public final a2\(ILcom/ss/android/ugc/aweme/feed/model/Aweme;\)V.*?^\.end method",selected_date,text)
            if ".class" in text and "Lcom/ss/android/ugc/aweme/autocaption/refactor/CLACaptionAssemV2;" in text:
                def caption_slot(match):
                    method=match.group(0)
                    if "FeedCaptions;->bindNative" in method:return method
                    method=re.sub(r"(?m)^.*invoke-static \{p1\}, Lcat/narezany/margyt/FeedCaptions;->bind\(Landroid/view/View;\)V\n","",method)
                    changed,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static/range {p0 .. p0}, Lcat/narezany/margyt/FeedCaptions;->bindNative(Ljava/lang/Object;)V\n"+r"\1",method)
                    if hits:counts["native subtitle slot"]=counts.get("native subtitle slot",0)+hits
                    return changed
                text=re.sub(r"(?ms)^\.method public final onViewCreated\(Landroid/view/View;\)V.*?^\.end method",caption_slot,text)
            if "Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem;" in text.split("\n",1)[0]:
                def speed_slot(match):
                    method=match.group(0)
                    if "FeedCaptions;->bindSpeed" in method:return method
                    changed,hits=re.subn(r"(?m)^([ \t]*return-void)","    invoke-static {p1}, Lcat/narezany/margyt/FeedCaptions;->bindSpeed(Landroid/view/View;)V\n"+r"\1",method)
                    if hits:counts["native speed feedback"]=counts.get("native speed feedback",0)+hits
                    return changed
                text=re.sub(r"(?ms)^\.method public final onViewCreated\(Landroid/view/View;\)V.*?^\.end method",speed_slot,text)
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts



def rewrite_native_flags(root: str) -> Dict[str, int]:
    counts = {}
    keys = ('"audio_comment_publish"','"profile_bg_in_allow_list"','"profile_bg_enable_consumption_group"')
    pattern = re.compile(r"invoke-virtual(?P<range>/range)? (?P<registers>\{[^}]+\}), LX/09tK;->LJIIJJI\(IILjava/lang/String;Z\)I")
    for directory, _, files in os.walk(root):
        for name in files:
            # On Windows, baksmali disambiguates class files whose descriptors
            # differ only by case by appending a numeric suffix (for example,
            # ``0A7j.1.smali``). Keep the exact class allow-list, but include
            # those deterministic collision suffixes so the profile banner
            # gate is not silently skipped.
            if name != 'AFwS227S0000000_3.smali' and not re.fullmatch(r'0A7j(?:\.\d+)?\.smali', name): continue
            path=os.path.join(directory,name)
            with open(path,encoding="utf-8") as handle: text=handle.read()
            def edit(match):
                method=match.group(0)
                if not any(key in method for key in keys): return method
                method,hits=pattern.subn(lambda m:"invoke-static"+(m.group('range') or '')+" "+m.group('registers')+", "+FLAGS+"->nativeInt(Ljava/lang/Object;IILjava/lang/String;Z)I",method)
                if hits: counts['native experiment overrides']=counts.get('native experiment overrides',0)+hits
                return method
            changed=re.sub(r"(?ms)^\.method .*?^\.end method",edit,text)
            if changed!=text:
                with open(path,"w",encoding="utf-8") as handle:handle.write(changed)
    return counts

LOADING_SPINNER = "Lcom/bytedance/tux/status/loading/TuxSpinner;"
LOADING_TARGET = "Lcat/narezany/margyt/LoadingIndicator;->draw(Landroid/view/View;Landroid/graphics/Canvas;)V"
LOADING_BALL_TARGET = "Lcat/narezany/margyt/LoadingIndicator;->draw(Landroid/view/View;Landroid/graphics/Canvas;IZ)V"
LOADING_LOTTIE = "LX/1HkT;"
LOADING_LOTTIE_TARGET = "Lcat/narezany/margyt/LoadingIndicator;->drawLottie(Landroid/view/View;Landroid/graphics/Canvas;Ljava/lang/Object;Z)Z"
# Verified against TikTok 47.2.41: four ball implementations and Tux's three dots.
# Tuple: preview flag, running flag, initialized flag, native indicator size.
LOADING_BALLS = {
    "LX/0DZv;": ("LLJJJJJIL", "LLJJJ", "LLJJJIL", "LLJJL"),
    "LX/0DZw;": (None, "LLJJIJIL", None, "LLJJJIL"),
    "LX/0fzn;": ("LLJLILLLLZIIL", "LLJJLIIIJLLLLLLLZ", "LLJL", "LLJLLIL"),
    "LX/0pWh;": ("LLJJJJ", "LLJJJ", "LLJJJIL", "LLJJJJLIIL"),
}
LOADING_CLASSES = (LOADING_SPINNER, "LX/0GdO;", *LOADING_BALLS, LOADING_LOTTIE)

def loading_feature(owner: str) -> str:
    return "native MD3 loader " + owner

def has_loading_class(dex: bytes) -> bool:
    return any(owner.encode() in dex for owner in LOADING_CLASSES)

def rewrite_loading_spinner(root: str) -> Dict[str, int]:
    """Replace every verified dot renderer while retaining native loading/preview gates."""
    counts = {}
    stems = {owner.rsplit("/",1)[-1].rstrip(";").lower() for owner in LOADING_CLASSES}
    pattern = r"(?ms)^(\.method (?:public|protected)[^\n]* onDraw\(Landroid/graphics/Canvas;\)V)\n.*?^\.end method"
    for folder, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali") or name.split(".")[0].lower() not in stems: continue
            path = os.path.join(folder,name)
            with open(path,encoding="utf-8") as handle: text=handle.read()
            match=re.search(r"(?m)^\.class[^\n]* (L[^\n]+;)$",text)
            owner=match.group(1) if match else None
            if owner not in LOADING_CLASSES: continue
            target=LOADING_LOTTIE_TARGET if owner==LOADING_LOTTIE else LOADING_BALL_TARGET if owner in LOADING_BALLS else LOADING_TARGET
            if target in text: continue
            if owner==LOADING_LOTTIE:
                # All ways to load a Lottie composition share this draw method,
                # including raw resources, InputStreams and XML attributes.
                body=("    .registers 6\n"
                      "    iget-object v0, p0, LX/1HkT;->composition:LX/1Hl1;\n"
                      "    invoke-virtual {p0}, LX/1HkT;->isAnimating()Z\n"
                      "    move-result v1\n"
                      "    invoke-static {p0, p1, v0, v1}, " + target + "\n"
                      "    move-result v0\n"
                      "    if-eqz v0, :ttcuz_native_lottie\n"
                      "    return-void\n"
                      "    :ttcuz_native_lottie\n"
                      "    invoke-static {p0, p1}, LX/1HkT;->com_airbnb_lottie_LottieAnimationView_com_bytedance_tt_reliability_monitor_viewchecker_BitmapCrashChecker_onDraw(LX/1HkT;Landroid/graphics/Canvas;)V\n"
                      "    return-void")
                for signature in (".field public composition:LX/1Hl1;", ".method public isAnimating()Z", "com_airbnb_lottie_LottieAnimationView_com_bytedance_tt_reliability_monitor_viewchecker_BitmapCrashChecker_onDraw(LX/1HkT;Landroid/graphics/Canvas;)V"):
                    if signature not in text: raise RuntimeError("Lottie loading contract changed: " + signature)
            elif owner in LOADING_BALLS:
                preview,running,ready,size=LOADING_BALLS[owner]
                for field in (preview,running,ready):
                    if field and not re.search(r"(?m)^\.field[^\n]* " + field + r":Z$",text): raise RuntimeError(owner+" loading flag changed: "+field)
                if not re.search(r"(?m)^\.field[^\n]* " + size + r":I$",text): raise RuntimeError(owner+" loading size changed")
                body="    .registers 5\n"
                if preview:
                    body+=("    iget-boolean v0, p0, "+owner+"->"+preview+":Z\n"
                           "    if-nez v0, :ttcuz_loading_ready\n"
                           "    iget-boolean v0, p0, "+owner+"->"+running+":Z\n"
                           "    if-eqz v0, :ttcuz_loading_ready\n"
                           "    return-void\n"
                           "    :ttcuz_loading_ready\n")
                gate=ready or running
                body+=("    iget-boolean v0, p0, "+owner+"->"+gate+":Z\n"
                       "    if-eqz v0, :ttcuz_loading_end\n"
                       "    iget v0, p0, "+owner+"->"+size+":I\n"
                       "    iget-boolean v1, p0, "+owner+"->"+running+":Z\n"
                       "    invoke-static {p0, p1, v0, v1}, "+target+"\n"
                       "    :ttcuz_loading_end\n"
                       "    return-void")
            else:
                body="    .registers 2\n    invoke-static/range {p0 .. p1}, "+target+"\n    return-void"
            text,hits=re.subn(pattern,lambda m:m.group(1)+"\n"+body+"\n.end method",text)
            if hits!=1:raise RuntimeError(owner+".onDraw changed; MD3 loading hook cannot be applied")
            with open(path,"w",encoding="utf-8") as handle:handle.write(text)
            counts[loading_feature(owner)]=hits
    return counts


def carries_an_anchor(dex: bytes) -> bool:
    return (has_loading_class(dex) or any(marker.encode() in dex for marker in TEXT_LIMIT_MARKERS) or b"Lcom/bytedance/tux/icon/TuxIconView;" in dex or b"streak_icon_inline_" in dex or any(anchor.encode() in dex for anchor, *_rest in ANCHORED_SOURCES)
            or b"draftProgressBar" in dex or b"can not show seekbar, state: 4" in dex
            or FEED_ITEM_LIST.encode() in dex
            or b"Lcom/ss/android/ugc/aweme/comment/longpresspanel/ui/CommentLongPressFragment;" in dex
            or b"LX/0wiL;" in dex or b"Lkotlin/jvm/internal/AwS704S0100000_26;" in dex
            or b"Lcom/ss/android/ugc/aweme/offlinemode/viewmodel/OfflineModeManagerVM;" in dex
            or b"Lcom/ss/android/ugc/aweme/comment/commentlist/api/CommentApi;" in dex
            or b"Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2;" in dex
            or b"Lcom/ss/android/ugc/aweme/commentv2/commentlist/CommentApiV2SplitJson;" in dex
            or b"LX/0oah;" in dex
            or b"LX/0wjq;" in dex
            or b"LX/0wk3;" in dex or b"Lcom/ss/android/ugc/aweme/offlinemode/ui/OfflineModeContentComponent;" in dex
            or b"Lcom/ss/android/ugc/aweme/offlinemode/ui/bottom/OfflineModeDetailBottomComponent;" in dex
            or b"Lcom/ss/android/ugc/aweme/offlinemode/ui/sheet/OfflineModeSheetPageAssem;" in dex
            or b"audio_comment_publish" in dex or b"profile_bg_in_allow_list" in dex
            or b"Lcom/ss/android/ugc/aweme/feed/assem/videoauthorinfo/VideoAuthorInfoRelationAssem;" in dex
            or any(owner.encode() in dex for owner in RAIL_CLASSES)
            or PLAYER_CONTROLLER.encode() in dex
            or any(owner.encode() in dex for owner in (REPOST_CLASS,AVATAR_CLASS,PROFILE_CONTAINER,PROFILE_HEADER_CONTAINER,PUSH_GUIDE)+MESSAGE_CLASSES)
            or b"Lcom/ss/android/ugc/aweme/autocaption/refactor/CLACaptionAssemV2;" in dex
            or b"Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAssem;" in dex
            or SPEED_TOUCH_CLASS.encode() in dex or SPEED_EDGE_CLASS.encode() in dex)


def rewrite_speed_touch(root: str) -> Dict[str, int]:
    """Observe the native edge-speed gesture and add swipe-down speed locking."""
    counts: Dict[str, int] = {}
    found_class = False
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if not text.startswith(".class "):
                continue
            declaration = text.split("\n", 1)[0]
            edge = SPEED_EDGE_CLASS in declaration
            if not edge and SPEED_TOUCH_CLASS not in declaration:
                continue
            label = "native edge speed lock" if edge else "swipe-down speed lock"
            found_class = True
            start = text.find(SPEED_EDGE_METHOD if edge else SPEED_TOUCH_METHOD)
            if start < 0:
                raise RuntimeError("TikTok feed touch callback has changed")
            end = text.find(".end method", start)
            if end < 0:
                raise RuntimeError("TikTok feed touch callback is incomplete")
            method = text[start:end]
            if SPEED_TOUCH_TARGET in method:
                counts[label] = 1
                continue
            anchor = "    :cond_9\n" if edge else "    :cond_27\n"
            if method.count(anchor) != 1:
                raise RuntimeError("TikTok feed touch callback anchor has changed")
            replacement = method.replace(anchor, anchor + SPEED_TOUCH_HOOK, 1)
            text = text[:start] + replacement + text[end:]
            with open(path, "w", encoding="utf-8") as handle:
                handle.write(text)
            counts[label] = 1
    return counts


def touches_a_model(dex: bytes) -> bool:
    """Whether a dex names one of TikTok's models and something we want on it."""
    for owner, name, _original, _replacement, _target in MODEL_SOURCES + MODEL_STATICS:
        theirs = name[0] if isinstance(name, tuple) else name
        if owner.encode() in dex and theirs.encode() in dex:
            return True
    for owner, field, _kind, _name, _target in FIELD_SOURCES:
        if owner.encode() in dex and field.encode() in dex:
            return True
    for name, _original, _replacement, _target in WILD_SOURCES:
        if name.encode() in dex:
            return True
    for label, _descriptor, _ours, _target in DISCOVERED_STATICS + DISCOVERED_VIRTUALS:
        found = FOUND.get(label)
        if found and found[0].encode() in dex and found[1].encode() in dex:
            return True
    return False


def reads_a_colour(dex: bytes) -> bool:
    """Whether a dex asks the framework for a colour.

    Most of TikTok's pink is not a constant at all: it is a colour resource,
    fetched by id, from code spread across most of the apk. Reaching it means
    opening every dex that asks -- which is most of them, and the reason a
    build takes a quarter of an hour rather than two minutes.
    """
    for owner, name, _original, _replacement in COLOUR_SOURCES:
        if owner.encode() in dex and name.encode() in dex:
            return True
    for owner, name, _signature in COLOUR_STATICS:
        if owner.encode() in dex and name.encode() in dex:
            return True
    return False


def holds_the_pink(dex: bytes) -> bool:
    """Whether a dex has the pink as a constant in an instruction.

    The four bytes of the colour turn up in string data and in tables too, and
    taking a dex apart costs half a minute -- so this looks for the instruction
    itself: opcode 0x14, `const vAA, #+BBBBBBBB`, the register, then the value.
    """
    needle = struct.pack("<I", TIKTOK_PINK)
    at = dex.find(needle)
    while at != -1:
        if at >= 2 and dex[at - 2] == 0x14:
            return True
        at = dex.find(needle, at + 1)
    return False


def _literal_of(pattern) -> str:
    """The plain text a rule cannot match without, or "" if there is none.

    Every rule looks for one method or one constant, so a file that does not
    spell it cannot match -- and checking that is a substring search, where
    running the rule is a regex over the whole file.
    """
    source = pattern.pattern
    for head in (r"(\{[^}]*\}), ", r"^(\s*)const ([vp]\d+), "):
        at = source.find(head)
        if at != -1:
            tail = source[at + len(head):]
            if tail.endswith("$"):
                tail = tail[:-1]
            return re.sub(r"\\(.)", r"\1", tail)
    return ""


def rewrite_accent(root: str) -> Dict[str, int]:
    """Rewrite everywhere the pink is written down, counting as it goes."""
    counts: Dict[str, int] = {}
    prepared = accent_rules()
    markers = [_literal_of(pattern) for _label, pattern, _target in prepared]
    feed_animation = "LX/1HkT;->setAnimation(Ljava/lang/String;)V"
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            # Protect colours at the source: restoring the returned drawable
            # cannot undo constants/getters already recoloured inside its factory.
            if '"streak_icon_inline_"' in text or (
                    text.split("\n",1)[0].endswith(";") and
                    "Lcom/ss/android/ugc/aweme/im/streak/" in text.split("\n",1)[0]):
                continue
            before = text
            for (label, pattern, target), marker in zip(prepared, markers):
                if marker and marker not in text:
                    continue
                if label == "feed Lottie setAnimation" and feed_animation not in text:
                    continue
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts


def force_false(root: str) -> Dict[str, int]:
    """Rewrite the methods in FORCED_FALSE to `return false`, body and all."""
    counts: Dict[str, int] = {}
    for class_name, signature in FORCED_FALSE:
        path = os.path.join(root, *class_name.split("/")) + ".smali"
        if not os.path.exists(path):
            continue
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        pattern = re.compile(
            r"^\.method ([^\n]*%s)\n.*?^\.end method$" % re.escape(signature),
            re.MULTILINE | re.DOTALL,
        )
        match = pattern.search(text)
        if match is None:
            raise RuntimeError(
                "%s is in the apk but has no %s to rewrite -- the fallback this "
                "depends on has moved, and Google sign-in would be dead on arrival"
                % (class_name, signature)
            )
        stub = ".method %s\n    .registers 1\n\n    const/4 v0, 0x0\n\n    return v0\n.end method" % (
            match.group(1),
        )
        text = text[: match.start()] + stub + text[match.end():]
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        counts["%s->%s" % (class_name.rsplit("/", 1)[-1], signature)] = 1
    return counts


def interesting(dex: bytes, literals: Optional[Dict[str, str]] = None) -> bool:
    """A quick look at the raw dex before spending a minute on it.

    Every method a dex calls and every string it holds is in its string table,
    so a dex that never spells `TelephonyManager` cannot be calling one of
    these, and one that never spells an authority cannot be looking it up.
    """
    for old in (literals or {}):
        if old.encode() in dex:
            return True
    if (b"tt_feed_like_tap_tux_lottie.json" in dex
            or b"tt_feed_like_tap_quick_tux_lottie.json" in dex
            or b"feed_fill_like/feed_fill_like_add_lottie.json" in dex):
        return True
    if (holds_the_pink(dex) or reads_a_colour(dex) or touches_a_model(dex)
            or carries_an_anchor(dex)):
        return True
    for class_name, _signature in FORCED_FALSE:
        if ("L%s;" % class_name).encode() in dex:
            return True
    if TELEPHONY.encode() not in dex:
        return False
    return any(name.encode() in dex for name, _o, _r in TARGETS)


def rewrite_literals(root: str, literals: Dict[str, str]) -> Dict[str, int]:
    """Swap whole string constants, for the authorities the manifest renamed.

    A provider authority renamed in the manifest and not in the code is an app
    that cannot find its own provider -- so if any of these strings turn out to
    be in the bytecode after all, they move with it.
    """
    counts: Dict[str, int] = {}
    if not literals:
        return counts
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            before = text
            for old, new in literals.items():
                needle = '"%s"' % old
                hits = text.count(needle)
                if hits:
                    text = text.replace(needle, '"%s"' % new)
                    counts[old] = counts.get(old, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts


def rewrite_smali(root: str) -> Dict[str, int]:
    """Rewrite every call site under `root`, counting them by signature."""
    counts: Dict[str, int] = {}
    prepared = rules()
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if TELEPHONY not in text:
                continue
            before = text
            for label, pattern, target in prepared:
                text, hits = pattern.subn(target, text)
                if hits:
                    counts[label] = counts.get(label, 0) + hits
            if text != before:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(text)
    return counts


class Smali:
    """baksmali and smali, from the one jar the build downloads."""

    def __init__(self, jar: str, api: int, jobs: int = 0, heap: str = "4g"):
        self.jar = jar
        self.api = api
        self.jobs = jobs or int(os.environ.get("MARGYT_JOBS") or 0) or (os.cpu_count() or 2)
        self.heap = os.environ.get("MARGYT_HEAP") or heap

    def _run(self, main: str, args: List[str]) -> None:
        command = ["java", "-Xmx" + self.heap, "-cp", self.jar, main] + args
        result = subprocess.run(command, capture_output=True, text=True)
        if result.returncode != 0:
            raise RuntimeError(
                "%s failed:\n%s\n%s" % (main.split(".")[-2], result.stdout, result.stderr)
            )

    def disassemble(self, dex_path: str, out_dir: str) -> None:
        self._run(
            "com.android.tools.smali.baksmali.Main",
            ["d", "-a", str(self.api), "-j", str(self.jobs), "-o", out_dir, dex_path],
        )

    def assemble(self, smali_dir: str, dex_path: str) -> None:
        self._run(
            "com.android.tools.smali.smali.Main",
            ["a", "-a", str(self.api), "-j", str(self.jobs), "-o", dex_path, smali_dir],
        )


def dex_format(dex: bytes) -> str:
    """The three digits after `dex\n`: 035, 038, 039 ..."""
    return dex[4:7].decode("ascii", "replace")


# --------------------------------------------------------- the landing sites
#
# Every rewrite above turns a call into the app's own code into a call into
# ours, and smali will assemble a call to a method that does not exist without
# a word: a dex may reference anything, and the runtime only goes looking when
# the instruction is reached. So a missing method is not a build failure, it is
# a NoSuchMethodError on whichever screen first draws that colour -- which is
# how `Accent.getColor(Context, int)`, rewritten at 33 dex files' worth of call
# sites and never written in Java, shipped once.


def rewrite_targets() -> List[str]:
    """Every static the rewrites point at, as `Lowner;->name(descriptor)`."""
    out = ["%s->accent()I" % ACCENT, LOADING_TARGET, LOADING_BALL_TARGET, LOADING_LOTTIE_TARGET]
    out.extend(TEXT_LIMITS + "->" + sig for sig in ("comment(I)I", "commentLong(J)J", "repost(I)I", "hashtags(I)I", "hashtagGate(Z)Z"))
    out.append("%s->setAnimation(Ljava/lang/Object;Ljava/lang/String;)V" % ACCENT)
    for name, _original, replacement in TARGETS:
        out.append("%s->%s%s" % (REGION, name, replacement))
    for _owner, name, _original, replacement in COLOUR_SOURCES:
        out.append("%s->%s%s" % (ACCENT, name, replacement))
    for _owner, name, signature in COLOUR_STATICS:
        out.append("%s->%s%s" % (ACCENT, name, signature))
    for _owner, name, _original, replacement, target in MODEL_SOURCES + MODEL_STATICS:
        _theirs, ours = name if isinstance(name, tuple) else (name, name)
        out.append("%s->%s%s" % (target, ours, replacement))
    for owner, _field, kind, name, target in FIELD_SOURCES:
        out.append("%s->%s(%s)%s" % (target, name, owner, kind))
    for _anchor, _owner, name, _original, replacement, target in ANCHORED_SOURCES:
        out.append("%s->%s%s" % (target, name, replacement))
    out.append("Lcat/narezany/margyt/VideoActions;->captureShare(Ljava/lang/Object;)V")
    out.extend(["Lcat/narezany/margyt/OfflineComments;->closed(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->nativeTask(Ljava/lang/String;)Ljava/lang/Object;", "Lcat/narezany/margyt/OfflineComments;->nativeObservable(Ljava/lang/String;)Ljava/lang/Object;"])
    out.extend(["Lcat/narezany/margyt/OfflineChoices;->counts(Ljava/util/List;)Ljava/util/List;", "Lcat/narezany/margyt/OfflineChoices;->row(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "Lcat/narezany/margyt/OfflineChoices;->validCount(Ljava/util/List;Ljava/lang/Object;)Z", "Lcat/narezany/margyt/OfflineChoices;->legacy(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->cached(Ljava/lang/Object;Ljava/lang/String;)V", "Lcat/narezany/margyt/OfflineComments;->content(Ljava/lang/Object;)V", "Lcat/narezany/margyt/OfflineComments;->bottom(Landroid/view/View;)Landroid/view/View;"])
    out.append(SPEED_TOUCH_TARGET)
    out.append(COMMENTS+"->mark(Landroid/view/View;)V")
    out.append(COMMENTS+"->openNativeActions(Ljava/lang/Object;)Z")
    out.append(COMMENTS+"->nativeItems(Ljava/lang/Object;Ljava/util/List;)Ljava/util/List;")
    out.append(FEED+"->withoutAds(Ljava/util/List;)Ljava/util/List;")
    out.append("Lcat/narezany/margyt/Repost;->textureChanged(Landroid/view/View;)V")
    out.append("Lcat/narezany/margyt/Accent;->dislike(Ljava/lang/Object;)V")
    out.append(COMMENTS+"->bindCell(Ljava/lang/Object;)V")
    out.append(RAIL+"->bindNative(Ljava/lang/Object;)V")
    out.append("Lcat/narezany/margyt/PlayerTools;->resumed(Ljava/lang/Object;Ljava/lang/String;)V")
    out.extend(["Lcat/narezany/margyt/Repost;->bind(Ljava/lang/Object;)V","Lcat/narezany/margyt/Repost;->anchor(Ljava/lang/Object;)V","Lcat/narezany/margyt/ProfileLayout;->bind(Ljava/lang/Object;Landroid/view/View;)V","Lcat/narezany/margyt/MessageTimes;->before(Ljava/lang/Object;)V","Lcat/narezany/margyt/MessageTimes;->bind(Ljava/lang/Object;Ljava/lang/Object;)V","Lcat/narezany/margyt/ModNotifications;->noPrompts()Z"])
    out.append("Lcat/narezany/margyt/ProfileLayout;->created(Ljava/lang/Object;)V")
    out.append(SEEKBAR+"->allow(Z)Z")
    out.append("Lcat/narezany/margyt/Accent;->keepNativeArtwork(Landroid/graphics/drawable/Drawable;)Landroid/graphics/drawable/Drawable;")
    out.append("Lcat/narezany/margyt/FeedCaptions;->bind(Landroid/view/View;)V")
    out.append("Lcat/narezany/margyt/FeedCaptions;->bindSpeed(Landroid/view/View;)V")
    out.append(FLAGS+"->nativeInt(Ljava/lang/Object;IILjava/lang/String;Z)I")
    out.append(DATES+"->bindAuthor(Ljava/lang/Object;)V")
    out.append(DATES+"->bindPost(Ljava/lang/Object;Ljava/lang/Object;)V")
    for name, _original, replacement, target in WILD_SOURCES:
        out.append("%s->%s%s" % (target, name, replacement))
    for label, descriptor, ours, target in DISCOVERED_STATICS:
        if label in FOUND:
            out.append("%s->%s%s" % (target, ours, descriptor))
    for label, descriptor, ours, target in DISCOVERED_VIRTUALS:
        if label in FOUND:
            out.append("%s->%s%s"
                       % (target, ours, "(Ljava/lang/Object;" + descriptor[1:]))
    return out


def _uleb(data: bytes, at: int) -> Tuple[int, int]:
    value = shift = 0
    while True:
        byte = data[at]
        at += 1
        value |= (byte & 0x7F) << shift
        shift += 7
        if not byte & 0x80:
            return value, at


def defined_methods(dex: bytes) -> set:
    """The methods a dex actually defines, as `Lowner;->name(descriptor)`.

    Referenced methods are not enough: the whole point of the check is that a
    reference to a method nobody wrote is exactly what the build has to catch.
    So this walks the class definitions rather than the method table.
    """
    string_ids_off = struct.unpack_from("<I", dex, 60)[0]
    type_ids_off = struct.unpack_from("<I", dex, 68)[0]
    proto_ids_off = struct.unpack_from("<I", dex, 76)[0]
    method_ids_off = struct.unpack_from("<I", dex, 92)[0]
    class_defs_size, class_defs_off = struct.unpack_from("<2I", dex, 96)

    def string(index: int) -> str:
        at = struct.unpack_from("<I", dex, string_ids_off + 4 * index)[0]
        length, at = _uleb(dex, at)
        return dex[at:at + length].decode("utf-8", "replace")

    def type_name(index: int) -> str:
        return string(struct.unpack_from("<I", dex, type_ids_off + 4 * index)[0])

    def descriptor(index: int) -> str:
        _shorty, return_type, parameters = struct.unpack_from(
            "<3I", dex, proto_ids_off + 12 * index)
        arguments = ""
        if parameters:
            count = struct.unpack_from("<I", dex, parameters)[0]
            arguments = "".join(
                type_name(struct.unpack_from("<H", dex, parameters + 4 + 2 * i)[0])
                for i in range(count)
            )
        return "(%s)%s" % (arguments, type_name(return_type))

    def signature(index: int) -> str:
        owner, proto, name = struct.unpack_from("<HHI", dex, method_ids_off + 8 * index)
        return "%s->%s%s" % (type_name(owner), string(name), descriptor(proto))

    out = set()
    for i in range(class_defs_size):
        data_off = struct.unpack_from("<I", dex, class_defs_off + 32 * i + 24)[0]
        if not data_off:
            continue
        counts = []
        at = data_off
        for _ in range(4):  # static fields, instance fields, direct, virtual
            value, at = _uleb(dex, at)
            counts.append(value)
        for _ in range(counts[0] + counts[1]):
            _diff, at = _uleb(dex, at)
            _flags, at = _uleb(dex, at)
        for methods in (counts[2], counts[3]):
            index = 0
            for step in range(methods):
                diff, at = _uleb(dex, at)
                _flags, at = _uleb(dex, at)
                _code, at = _uleb(dex, at)
                index = diff if step == 0 else index + diff
                out.add(signature(index))
    return out


def find_statics(dexes: Dict[str, bytes]) -> Dict[str, Tuple[str, str]]:
    """Find each wanted static by its signature, across the whole apk.

    A descriptor is matched, not a name, so what comes back is whatever the
    obfuscator called it this release. Anything that matches in more than one
    class is dropped rather than guessed at: a rule that lands in the wrong
    place is worse than one that does not land at all.
    """
    wanted = {descriptor: label for label, descriptor, _ours, _target
              in DISCOVERED_STATICS + DISCOVERED_VIRTUALS}
    seen: Dict[str, set] = {label: set() for label in wanted.values()}

    for dex in dexes.values():
        for owner, name, descriptor in _static_methods(dex):
            label = wanted.get(descriptor)
            if label is not None:
                seen[label].add((owner, name))

    out: Dict[str, Tuple[str, str]] = {}
    for label, found in seen.items():
        if len(found) == 1:
            out[label] = next(iter(found))
    return out


def _static_methods(dex: bytes):
    """Every method the dex names, as (owner, name, descriptor)."""
    strings = struct.unpack_from("<I", dex, 60)[0]
    types = struct.unpack_from("<I", dex, 68)[0]
    protos = struct.unpack_from("<I", dex, 76)[0]
    count, methods = struct.unpack_from("<2I", dex, 88)

    def text(index: int) -> str:
        at = struct.unpack_from("<I", dex, strings + 4 * index)[0]
        size, at = _uleb(dex, at)
        return dex[at:at + size].decode("utf-8", "replace")

    def kind(index: int) -> str:
        return text(struct.unpack_from("<I", dex, types + 4 * index)[0])

    def shape(index: int) -> str:
        _shorty, ret, params = struct.unpack_from("<3I", dex, protos + 12 * index)
        taken = ""
        if params:
            how_many = struct.unpack_from("<I", dex, params)[0]
            taken = "".join(kind(struct.unpack_from("<H", dex, params + 4 + 2 * i)[0])
                            for i in range(how_many))
        return "(%s)%s" % (taken, kind(ret))

    for i in range(count):
        owner, proto, name = struct.unpack_from("<HHI", dex, methods + 8 * i)
        yield kind(owner), text(name), shape(proto)


def missing_targets(dex: bytes) -> List[str]:
    """Which of the rewrites' landing sites the mod's own dex does not define."""
    defined = defined_methods(dex)
    return [target for target in rewrite_targets() if target not in defined]


def patch(dex: bytes, name: str, smali: Smali, workspace: str,
          literals: Optional[Dict[str, str]] = None) -> Tuple[bytes, Dict[str, int]]:
    """Take one dex apart, rewrite what is in it, put it back together."""
    # a room of its own per dex, and never under a name something else uses:
    # the mod's own dex is called classes.dex too
    room = os.path.join(workspace, name.replace(".dex", ""))
    shutil.rmtree(room, ignore_errors=True)
    os.makedirs(room, exist_ok=True)

    dex_in = os.path.join(room, "in.dex")
    dex_out = os.path.join(room, "out.dex")
    with open(dex_in, "wb") as handle:
        handle.write(dex)

    smali.disassemble(dex_in, os.path.join(room, "smali"))
    counts = rewrite_smali(os.path.join(room, "smali"))
    counts.update(rewrite_literals(os.path.join(room, "smali"), literals or {}))
    counts.update(force_false(os.path.join(room, "smali")))
    counts.update(rewrite_models(os.path.join(room, "smali")))
    counts.update(rewrite_native_flags(os.path.join(room,"smali")))
    # Class-specific heart hooks must claim the feed heart's Tux tint calls
    # before the generic accent pass rewrites every TuxIconView tint. The
    # latter would otherwise swallow the exact native heart hook and leave
    # the build gate reporting a missing heart-tint patch.
    counts.update(rewrite_anchored(os.path.join(room, "smali")))
    counts.update(rewrite_accent(os.path.join(room, "smali")))
    counts.update(rewrite_speed_touch(os.path.join(room, "smali")))
    counts.update(rewrite_loading_spinner(os.path.join(room, "smali")))
    if not counts:
        shutil.rmtree(room, ignore_errors=True)
        return dex, counts

    smali.assemble(os.path.join(room, "smali"), dex_out)
    with open(dex_out, "rb") as handle:
        patched = handle.read()
    shutil.rmtree(room, ignore_errors=True)

    if dex_format(patched) != dex_format(dex):
        raise RuntimeError(
            "%s came back as dex %s, it went in as dex %s -- an Android old "
            "enough to be in the apk's minSdk would refuse to load it"
            % (name, dex_format(patched), dex_format(dex))
        )
    return patched, counts


def next_dex_name(names: List[str]) -> str:
    """The name a new dex has to take to be loaded: the next in the run.

    The runtime loads classes.dex, then classes2.dex, and stops at the first
    number that is missing -- so an extra dex is only read if it continues the
    sequence.
    """
    used = set()
    for name in names:
        match = re.fullmatch(r"classes(\d*)\.dex", name)
        if match:
            used.add(int(match.group(1) or "1"))
    number = 2
    while number in used:
        number += 1
    return "classes%d.dex" % number
