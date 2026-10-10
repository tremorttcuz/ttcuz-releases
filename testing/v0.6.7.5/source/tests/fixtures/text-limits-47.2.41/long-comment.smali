.class public Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/PortraitInputKeyboard;
.super Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/BaseInputKeyboard;
.source "SourceFile"

# interfaces
.implements Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/IKeyboardProtocol;
.implements Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/IKeyboarInputAbility;
.implements Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/IImageEditAbility;
.implements Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/IAudioEditAbility;
.implements LX/0bHa;
.implements LX/0Omy;


# static fields
.field public static final synthetic LLZIL:[LX/1Kzn;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "[",
            "LX/1Kzn<",
            "Ljava/lang/Object;",
            ">;"
        }
    .end annotation
.end field

.field public static final LLZILL:Ljava/util/Set;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Set<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field


# instance fields
.field public LLLILZLLLI:Lcom/ss/android/ugc/aweme/commentv2/keyboard/inboxquickcomment/ISingleCommentKeyboardAbility;

.field public final LLLIZZ:I

.field public final LLLJ:Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/PortraitInputKeyboard;

.field public LLLJIL:Landroid/view/View;

.field public LLLJL:LX/0HxL;

.field public LLLL:LX/0HvS;

.field public LLLLII:Landroid/view/View;

.field public LLLLIIIILLL:Lcom/bytedance/lighten/loader/SmartImageView;

.field public LLLLIIL:Landroid/view/ViewGroup;

.field public LLLLIILL:LX/0Hdt;

.field public final LLLLIILLL:LX/01SE;

.field public final LLLLIL:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "LX/17HK<",
            "Ljava/lang/Void;",
            ">;>;"
        }
    .end annotation
.end field

.field public LLLLILI:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field public LLLLJ:Landroid/widget/FrameLayout;

.field public LLLLJI:Lcom/bytedance/tux/input/TuxTextView;

.field public LLLLL:LX/0Y9m;

.field public LLLLLIL:LX/0Y9m;

.field public LLLLLILLIL:LX/0Y9m;

.field public LLLLLJIL:LX/0oiy;

.field public LLLLLJLJLL:Landroid/view/View;

.field public LLLLLL:Landroid/view/View;

.field public LLLLLLIL:Landroid/view/View;

.field public LLLLLLJ:Landroid/view/View;

.field public LLLLLLL:Landroid/view/View;

.field public LLLLLLLLL:I

.field public LLLLLLLLLL:I

.field public LLLLLLLZIL:Z

.field public LLLLLLZ:Z

.field public LLLLLLZZ:Z

.field public LLLLLZ:Z

.field public LLLLLZIL:Z

.field public LLLLLZL:Z

.field public LLLLZ:Z

.field public LLLLZI:Z

.field public LLLLZIL:Z

.field public LLLLZLL:Z

.field public LLLLZLLIL:Lkotlin/Pair;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lkotlin/Pair<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field public LLLLZLLLI:Z

.field public LLLZ:Landroid/content/Intent;

.field public final LLLZI:LX/0odv;

.field public final LLLZIIL:Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/PortraitInputKeyboard$handler$1;

.field public final LLLZIL:LX/0ocx;

.field public final LLLZL:Lcom/bytedance/assem/arch/viewModel/AssemVMLazy;

.field public final LLLZLL:LX/01SE;

.field public LLLZLZ:Lcom/ss/android/ugc/aweme/comment/model/Comment;

.field public final LLLZZ:Ljava/util/concurrent/atomic/AtomicBoolean;

.field public LLLZZIL:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/ss/android/ugc/aweme/comment/model/CommentImageModel;",
            ">;"
        }
    .end annotation
.end field

.field public LLZ:Z


# direct methods

.method public final wr()I
    .registers 4

    .prologue
    .line 0
    invoke-static {}, LX/0ods;->LIZIZ()Z

    .line 1
    .line 2
    .line 3
    move-result v0

    .line 4
    if-eqz v0, :cond_14

    .line 5
    .line 6
    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/BaseInputAssem;->Oq()Z

    .line 7
    .line 8
    .line 9
    move-result v0

    .line 10
    if-nez v0, :cond_14

    .line 11
    .line 12
    invoke-static {}, LX/0ods;->LIZ()Lcom/ss/android/ugc/aweme/comment/experiment/LongCommentWritingConfig;

    .line 13
    .line 14
    .line 15
    move-result-object v0

    .line 16
    iget-wide v1, v0, Lcom/ss/android/ugc/aweme/comment/experiment/LongCommentWritingConfig;->maxInputLimit:J

    .line 17
    .line 18
    long-to-int v0, v1

    .line 19
    return v0

    .line 20
    :cond_14
    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/comment/keyboard/keyboardv2/refactor/BaseInputAssem;->Bq()Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;

    .line 21
    .line 22
    .line 23
    move-result-object v0

    .line 24
    if-eqz v0, :cond_23

    .line 25
    .line 26
    invoke-static {v0}, Lcom/ss/android/ugc/aweme/comment/model/CommentContextSourceKt;->aweme(Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;)Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 27
    .line 28
    .line 29
    move-result-object v0

    .line 30
    if-eqz v0, :cond_23

    .line 31
    .line 32
    invoke-static {v0}, Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;->getAuthorUid(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 33
    .line 34
    .line 35
    :cond_23
    invoke-static {}, LX/0onX;->LIZIZ()I

    .line 36
    .line 37
    .line 38
    move-result v0

    .line 39
    return v0
.end method
