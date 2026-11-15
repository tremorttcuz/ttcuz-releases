.class public final LX/08Xq;
.super Ljava/lang/Object;
.source "SourceFile"


# direct methods
.method public static final LIZ(Landroid/view/View;Lkotlin/jvm/functions/Function1;)V
    .registers 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/view/View;",
            "Lkotlin/jvm/functions/Function1<",
            "-",
            "Landroid/view/View;",
            "Ljava/lang/Boolean;",
            ">;)V"
        }
    .end annotation

    .prologue
    .line 33554432
    new-instance v0, LX/0Ykf;

    .line 33554433
    .line 33554434
    invoke-direct {v0, p1}, LX/0Ykf;-><init>(Lkotlin/jvm/functions/Function1;)V

    .line 33554435
    .line 33554436
    .line 33554437
    invoke-virtual {p0, v0}, Landroid/view/View;->addOnLayoutChangeListener(Landroid/view/View$OnLayoutChangeListener;)V

    .line 33554438
    .line 33554439
    .line 33554440
    return-void
.end method

.method public static final LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z
    .registers 4

    .prologue
    .line 16777216
    const/4 v2, 0x0

    .line 16777217
    if-eqz p0, :cond_6f

    .line 16777218
    .line 16777219
    invoke-static {p0}, LX/0B8N;->LJFF(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777220
    .line 16777221
    .line 16777222
    move-result v0

    .line 16777223
    if-nez v0, :cond_6f

    .line 16777224
    .line 16777225
    invoke-static {p0}, LX/0A0B;->LJII(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777226
    .line 16777227
    .line 16777228
    move-result v0

    .line 16777229
    if-nez v0, :cond_6f

    .line 16777230
    .line 16777231
    invoke-static {p0}, LX/0A0B;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    if-nez v0, :cond_6f

    .line 16777236
    .line 16777237
    invoke-static {p0}, LX/0A0B;->LJIIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777238
    .line 16777239
    .line 16777240
    move-result v0

    .line 16777241
    if-nez v0, :cond_6f

    .line 16777242
    .line 16777243
    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAwemeRawAd()Lcom/ss/android/ugc/aweme/feed/model/AwemeRawAd;

    .line 16777244
    .line 16777245
    .line 16777246
    move-result-object v0

    .line 16777247
    if-eqz v0, :cond_33

    .line 16777248
    .line 16777249
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 16777250
    .line 16777251
    .line 16777252
    move-result-object v1

    .line 16777253
    const-class v0, Lcom/ss/android/ugc/aweme/commercialize/promote/interfaces/ICommercePromoteService;

    .line 16777254
    .line 16777255
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 16777256
    .line 16777257
    .line 16777258
    move-result-object v0

    .line 16777259
    check-cast v0, Lcom/ss/android/ugc/aweme/commercialize/promote/interfaces/ICommercePromoteService;

    .line 16777260
    .line 16777261
    invoke-interface {v0, p0}, Lcom/ss/android/ugc/aweme/commercialize/promote/interfaces/ICommercePromoteService;->LJIJJLI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777262
    .line 16777263
    .line 16777264
    move-result v0

    .line 16777265
    if-nez v0, :cond_6f

    .line 16777266
    .line 16777267
    :cond_33
    invoke-static {p0}, Lcom/ss/android/ugc/aweme/feed/model/AwemeSaaSExtKt;->isLive(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777268
    .line 16777269
    .line 16777270
    move-result v0

    .line 16777271
    if-nez v0, :cond_6f

    .line 16777272
    .line 16777273
    invoke-static {p0}, Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;->isPhotoMode(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777274
    .line 16777275
    .line 16777276
    move-result v0

    .line 16777277
    if-nez v0, :cond_6f

    .line 16777278
    .line 16777279
    sget-object v0, LX/0B7G;->LIZ:LX/0B7G;

    .line 16777280
    .line 16777281
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777282
    .line 16777283
    .line 16777284
    invoke-static {p0}, LX/0B7G;->LJFF(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777285
    .line 16777286
    .line 16777287
    move-result v0

    .line 16777288
    if-nez v0, :cond_6f

    .line 16777289
    .line 16777290
    sget-object v1, LX/0BHc;->LIZ:LX/02pj;

    .line 16777291
    .line 16777292
    invoke-interface {v1}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 16777293
    .line 16777294
    .line 16777295
    move-result-object v0

    .line 16777296
    check-cast v0, Ljava/lang/Boolean;

    .line 16777297
    .line 16777298
    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    .line 16777299
    .line 16777300
    .line 16777301
    move-result v0

    .line 16777302
    if-nez v0, :cond_6e

    .line 16777303
    .line 16777304
    invoke-interface {v1}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 16777305
    .line 16777306
    .line 16777307
    move-result-object v0

    .line 16777308
    check-cast v0, Ljava/lang/Boolean;

    .line 16777309
    .line 16777310
    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    .line 16777311
    .line 16777312
    .line 16777313
    move-result v0

    .line 16777314
    if-nez v0, :cond_6f

    .line 16777315
    .line 16777316
    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideoControl()Lcom/ss/android/ugc/aweme/feed/model/VideoControl;

    .line 16777317
    .line 16777318
    .line 16777319
    move-result-object v0

    .line 16777320
    if-eqz v0, :cond_6f

    .line 16777321
    .line 16777322
    iget v0, v0, Lcom/ss/android/ugc/aweme/feed/model/VideoControl;->draftProgressBar:I

    .line 16777323
    .line 16777324
    if-eqz v0, :cond_6f

    .line 16777325
    .line 16777326
    :cond_6e
    const/4 v2, 0x1

    .line 16777327
    :cond_6f
    return v2
.end method

.method public static final LIZJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z
    .registers 3

    .prologue
    .line 16777216
    const/4 v1, 0x0

    .line 16777217
    if-eqz p0, :cond_e

    .line 16777218
    .line 16777219
    invoke-virtual {p0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideoControl()Lcom/ss/android/ugc/aweme/feed/model/VideoControl;

    .line 16777220
    .line 16777221
    .line 16777222
    move-result-object v0

    .line 16777223
    if-eqz v0, :cond_e

    .line 16777224
    .line 16777225
    iget v0, v0, Lcom/ss/android/ugc/aweme/feed/model/VideoControl;->showProgressBar:I

    .line 16777226
    .line 16777227
    if-eqz v0, :cond_e

    .line 16777228
    .line 16777229
    const/4 v1, 0x1

    .line 16777230
    :cond_e
    return v1
.end method

.method public static final LIZLLL(Landroidx/fragment/app/Fragment;)Z
    .registers 3

    .prologue
    .line 16777216
    instance-of v0, p0, LX/05DI;

    .line 16777217
    .line 16777218
    if-eqz v0, :cond_74

    .line 16777219
    .line 16777220
    check-cast p0, LX/05DI;

    .line 16777221
    .line 16777222
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777223
    .line 16777224
    .line 16777225
    move-result-object v1

    .line 16777226
    const-string v0, "FeedFollowFragment"

    .line 16777227
    .line 16777228
    invoke-static {v0, v1}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777229
    .line 16777230
    .line 16777231
    move-result v0

    .line 16777232
    if-nez v0, :cond_72

    .line 16777233
    .line 16777234
    const-string v1, "FeedRecommendFragment"

    .line 16777235
    .line 16777236
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777237
    .line 16777238
    .line 16777239
    move-result-object v0

    .line 16777240
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777241
    .line 16777242
    .line 16777243
    move-result v0

    .line 16777244
    if-nez v0, :cond_72

    .line 16777245
    .line 16777246
    const-string v1, "FeedLearnFragment"

    .line 16777247
    .line 16777248
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777249
    .line 16777250
    .line 16777251
    move-result-object v0

    .line 16777252
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777253
    .line 16777254
    .line 16777255
    move-result v0

    .line 16777256
    if-nez v0, :cond_72

    .line 16777257
    .line 16777258
    const-string v1, "FeedPopularFragment"

    .line 16777259
    .line 16777260
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777261
    .line 16777262
    .line 16777263
    move-result-object v0

    .line 16777264
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777265
    .line 16777266
    .line 16777267
    move-result v0

    .line 16777268
    if-nez v0, :cond_72

    .line 16777269
    .line 16777270
    const-string v1, "NearByFragment"

    .line 16777271
    .line 16777272
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777273
    .line 16777274
    .line 16777275
    move-result-object v0

    .line 16777276
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777277
    .line 16777278
    .line 16777279
    move-result v0

    .line 16777280
    if-nez v0, :cond_72

    .line 16777281
    .line 16777282
    const-string v1, "SocialFriendsFeedFragment"

    .line 16777283
    .line 16777284
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777285
    .line 16777286
    .line 16777287
    move-result-object v0

    .line 16777288
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777289
    .line 16777290
    .line 16777291
    move-result v0

    .line 16777292
    if-nez v0, :cond_72

    .line 16777293
    .line 16777294
    const-string v1, "RepostFeedFragment"

    .line 16777295
    .line 16777296
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777297
    .line 16777298
    .line 16777299
    move-result-object v0

    .line 16777300
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777301
    .line 16777302
    .line 16777303
    move-result v0

    .line 16777304
    if-nez v0, :cond_72

    .line 16777305
    .line 16777306
    sget-object v1, LX/0450;->LIZIZ:LX/0450;

    .line 16777307
    .line 16777308
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777309
    .line 16777310
    .line 16777311
    move-result-object v0

    .line 16777312
    invoke-virtual {v1, v0}, LX/0450;->LJJJI(Ljava/lang/String;)Z

    .line 16777313
    .line 16777314
    .line 16777315
    move-result v0

    .line 16777316
    if-nez v0, :cond_72

    .line 16777317
    .line 16777318
    sget-object v1, LX/06ks;->LIZIZ:LX/06ks;

    .line 16777319
    .line 16777320
    invoke-interface {p0}, LX/05DI;->Eo()Ljava/lang/String;

    .line 16777321
    .line 16777322
    .line 16777323
    move-result-object v0

    .line 16777324
    invoke-virtual {v1, v0}, LX/06ks;->e8(Ljava/lang/String;)Z

    .line 16777325
    .line 16777326
    .line 16777327
    move-result v0

    .line 16777328
    if-eqz v0, :cond_74

    .line 16777329
    .line 16777330
    :cond_72
    const/4 v0, 0x1

    .line 16777331
    return v0

    .line 16777332
    :cond_74
    const/4 v0, 0x0

    .line 16777333
    return v0
.end method

.method public static final LJ(Ljava/lang/String;)Z
    .registers 4

    .prologue
    .line 16777216
    const-string v0, "homepage_hot"

    .line 16777217
    .line 16777218
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777219
    .line 16777220
    .line 16777221
    move-result v0

    .line 16777222
    const/4 v2, 0x1

    .line 16777223
    if-nez v0, :cond_72

    .line 16777224
    .line 16777225
    const-string v0, "homepage_playmode_new"

    .line 16777226
    .line 16777227
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777228
    .line 16777229
    .line 16777230
    move-result v0

    .line 16777231
    if-nez v0, :cond_72

    .line 16777232
    .line 16777233
    const-string v0, "homepage_playmode_top"

    .line 16777234
    .line 16777235
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777236
    .line 16777237
    .line 16777238
    move-result v0

    .line 16777239
    if-nez v0, :cond_72

    .line 16777240
    .line 16777241
    const-string v0, "homepage_playmode_moodboost"

    .line 16777242
    .line 16777243
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777244
    .line 16777245
    .line 16777246
    move-result v0

    .line 16777247
    if-nez v0, :cond_72

    .line 16777248
    .line 16777249
    const-string v0, "homepage_follow"

    .line 16777250
    .line 16777251
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777252
    .line 16777253
    .line 16777254
    move-result v0

    .line 16777255
    if-nez v0, :cond_72

    .line 16777256
    .line 16777257
    const-string v0, "homepage_learn"

    .line 16777258
    .line 16777259
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777260
    .line 16777261
    .line 16777262
    move-result v0

    .line 16777263
    if-nez v0, :cond_72

    .line 16777264
    .line 16777265
    const-string v0, "homepage_friends"

    .line 16777266
    .line 16777267
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777268
    .line 16777269
    .line 16777270
    move-result v0

    .line 16777271
    if-nez v0, :cond_72

    .line 16777272
    .line 16777273
    const-string v0, "homepage_popular"

    .line 16777274
    .line 16777275
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777276
    .line 16777277
    .line 16777278
    move-result v0

    .line 16777279
    if-nez v0, :cond_72

    .line 16777280
    .line 16777281
    sget-object v1, LX/06ks;->LIZIZ:LX/06ks;

    .line 16777282
    .line 16777283
    if-nez p0, :cond_81

    .line 16777284
    .line 16777285
    const-string v0, ""

    .line 16777286
    .line 16777287
    :goto_47
    invoke-virtual {v1, v0}, LX/06ks;->f8(Ljava/lang/String;)Z

    .line 16777288
    .line 16777289
    .line 16777290
    move-result v0

    .line 16777291
    if-nez v0, :cond_72

    .line 16777292
    .line 16777293
    const-string v0, "homepage_nearby"

    .line 16777294
    .line 16777295
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777296
    .line 16777297
    .line 16777298
    move-result v0

    .line 16777299
    if-nez v0, :cond_72

    .line 16777300
    .line 16777301
    invoke-static {p0}, LX/09la;->LJII(Ljava/lang/String;)Z

    .line 16777302
    .line 16777303
    .line 16777304
    move-result v0

    .line 16777305
    if-nez v0, :cond_72

    .line 16777306
    .line 16777307
    invoke-static {p0}, LX/09la;->LIZ(Ljava/lang/String;)Z

    .line 16777308
    .line 16777309
    .line 16777310
    move-result v0

    .line 16777311
    if-nez v0, :cond_72

    .line 16777312
    .line 16777313
    sget-object v0, LX/0450;->LIZIZ:LX/0450;

    .line 16777314
    .line 16777315
    invoke-virtual {v0, p0}, LX/0450;->LJJIIZI(Ljava/lang/String;)Z

    .line 16777316
    .line 16777317
    .line 16777318
    move-result v0

    .line 16777319
    if-nez v0, :cond_72

    .line 16777320
    .line 16777321
    const-string/jumbo v0, "repost_feed"

    .line 16777322
    .line 16777323
    .line 16777324
    invoke-static {v0, p0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777325
    .line 16777326
    .line 16777327
    move-result v0

    .line 16777328
    if-eqz v0, :cond_83

    .line 16777329
    .line 16777330
    :cond_72
    invoke-static {}, LX/03AW;->LJFF()LX/05ue;

    .line 16777331
    .line 16777332
    .line 16777333
    move-result-object v0

    .line 16777334
    if-eqz v0, :cond_7f

    .line 16777335
    .line 16777336
    iget-object v0, v0, LX/05ue;->LIZ:Ljava/lang/Object;

    .line 16777337
    .line 16777338
    :goto_7a
    instance-of v0, v0, LX/047X;

    .line 16777339
    .line 16777340
    if-eqz v0, :cond_83

    .line 16777341
    .line 16777342
    return v2

    .line 16777343
    :cond_7f
    const/4 v0, 0x0

    .line 16777344
    goto :goto_7a

    .line 16777345
    :cond_81
    move-object v0, p0

    .line 16777346
    goto :goto_47

    .line 16777347
    :cond_83
    const/4 v2, 0x0

    .line 16777348
    return v2
.end method

.method public static final LJFF(ILandroid/view/View;)V
    .registers 4

    .prologue
    .line 33554432
    invoke-virtual {p1}, Landroid/view/View;->getPaddingTop()I

    .line 33554433
    .line 33554434
    .line 33554435
    move-result v1

    .line 33554436
    invoke-virtual {p1}, Landroid/view/View;->getPaddingBottom()I

    .line 33554437
    .line 33554438
    .line 33554439
    move-result v0

    .line 33554440
    invoke-virtual {p1, p0, v1, p0, v0}, Landroid/view/View;->setPadding(IIII)V

    .line 33554441
    .line 33554442
    .line 33554443
    return-void
.end method

.method public static final LJI(Landroid/view/View;IIII)V
    .registers 9

    .prologue
    .line 83886080
    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 83886081
    .line 83886082
    .line 83886083
    move-result-object v3

    .line 83886084
    move-object v2, v3

    .line 83886085
    check-cast v2, Landroid/widget/FrameLayout$LayoutParams;

    .line 83886086
    .line 83886087
    invoke-static {v3, v2}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 83886088
    .line 83886089
    .line 83886090
    move-result v0

    .line 83886091
    if-eqz v0, :cond_19

    .line 83886092
    .line 83886093
    iput p2, v2, Landroid/widget/FrameLayout$LayoutParams;->width:I

    .line 83886094
    .line 83886095
    iput p1, v2, Landroid/widget/FrameLayout$LayoutParams;->height:I

    .line 83886096
    .line 83886097
    iput p4, v2, Landroid/widget/FrameLayout$LayoutParams;->gravity:I

    .line 83886098
    .line 83886099
    iput p3, v2, Landroid/widget/FrameLayout$LayoutParams;->bottomMargin:I

    .line 83886100
    .line 83886101
    :goto_15
    invoke-static {p0, v3}, LX/00kD;->Z5(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    .line 83886102
    .line 83886103
    .line 83886104
    return-void

    .line 83886105
    :cond_19
    move-object v1, v3

    .line 83886106
    check-cast v1, Landroid/widget/RelativeLayout$LayoutParams;

    .line 83886107
    .line 83886108
    invoke-static {v3, v1}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 83886109
    .line 83886110
    .line 83886111
    move-result v0

    .line 83886112
    if-eqz v0, :cond_3b

    .line 83886113
    .line 83886114
    iput p2, v2, Landroid/widget/FrameLayout$LayoutParams;->width:I

    .line 83886115
    .line 83886116
    iput p1, v2, Landroid/widget/FrameLayout$LayoutParams;->height:I

    .line 83886117
    .line 83886118
    and-int/lit8 v0, p4, 0x50

    .line 83886119
    .line 83886120
    if-eqz v0, :cond_2f

    .line 83886121
    .line 83886122
    const/16 v0, 0xc

    .line 83886123
    .line 83886124
    invoke-virtual {v1, v0}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(I)V

    .line 83886125
    .line 83886126
    .line 83886127
    :cond_2f
    and-int/lit8 v0, p4, 0x1

    .line 83886128
    .line 83886129
    if-eqz v0, :cond_38

    .line 83886130
    .line 83886131
    const/16 v0, 0xe

    .line 83886132
    .line 83886133
    invoke-virtual {v1, v0}, Landroid/widget/RelativeLayout$LayoutParams;->addRule(I)V

    .line 83886134
    .line 83886135
    .line 83886136
    :cond_38
    iput p3, v2, Landroid/widget/FrameLayout$LayoutParams;->bottomMargin:I

    .line 83886137
    .line 83886138
    goto :goto_15

    .line 83886139
    :cond_3b
    iput p2, v2, Landroid/widget/FrameLayout$LayoutParams;->width:I

    .line 83886140
    .line 83886141
    iput p1, v2, Landroid/widget/FrameLayout$LayoutParams;->height:I

    .line 83886142
    .line 83886143
    iput p3, v2, Landroid/widget/FrameLayout$LayoutParams;->bottomMargin:I

    .line 83886144
    .line 83886145
    goto :goto_15
.end method
