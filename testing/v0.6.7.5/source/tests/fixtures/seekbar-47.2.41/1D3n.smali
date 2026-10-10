.class public final LX/1D3n;
.super Ljava/lang/Object;

.method public final LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z
    .registers 7

    .prologue
    .line 16777216
    sget-object v3, LX/138d;->LIZIZ:LX/138d;

    .line 16777217
    .line 16777218
    invoke-virtual {v3}, LX/138d;->LJJIJIIJI()LX/12ta;

    .line 16777219
    .line 16777220
    .line 16777221
    move-result-object v1

    .line 16777222
    iget-object v0, p0, LX/1D3n;->LLLFFI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777223
    .line 16777224
    const/4 v2, 0x0

    .line 16777225
    if-eqz v0, :cond_1e

    .line 16777226
    .line 16777227
    invoke-interface {v0}, LX/0Mrc;->getFragment()Landroidx/fragment/app/Fragment;

    .line 16777228
    .line 16777229
    .line 16777230
    move-result-object v0

    .line 16777231
    :goto_f
    invoke-interface {v1, v0}, LX/12ta;->LJJIJIIJIL(Landroidx/fragment/app/Fragment;)Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    const/4 v4, 0x0

    .line 16777236
    if-eqz v0, :cond_20

    .line 16777237
    .line 16777238
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777239
    .line 16777240
    const-string v0, "can not show seekbar, in story immersiveFeed"

    .line 16777241
    .line 16777242
    invoke-virtual {v1, v0, p1}, LX/0VdL;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777243
    .line 16777244
    .line 16777245
    return v4

    .line 16777246
    :cond_1e
    move-object v0, v2

    .line 16777247
    goto :goto_f

    .line 16777248
    :cond_20
    invoke-virtual {v3}, LX/138d;->LJJIJIIJI()LX/12ta;

    .line 16777249
    .line 16777250
    .line 16777251
    move-result-object v1

    .line 16777252
    iget-object v0, p0, LX/1D3n;->LLLFFI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777253
    .line 16777254
    if-eqz v0, :cond_2c

    .line 16777255
    .line 16777256
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->LJLLI()LX/139X;

    .line 16777257
    .line 16777258
    .line 16777259
    move-result-object v2

    .line 16777260
    :cond_2c
    invoke-interface {v1, v2}, LX/12ta;->LJIJJ(Ljava/lang/Object;)Z

    .line 16777261
    .line 16777262
    .line 16777263
    move-result v0

    .line 16777264
    if-eqz v0, :cond_3a

    .line 16777265
    .line 16777266
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777267
    .line 16777268
    const-string v0, "can not show seekbar, in story guide card"

    .line 16777269
    .line 16777270
    invoke-virtual {v1, v0}, LX/0VdL;->LJIIJ(Ljava/lang/String;)V

    .line 16777271
    .line 16777272
    .line 16777273
    return v4

    .line 16777274
    :cond_3a
    iget v0, p0, LX/1D3n;->LLJLLIL:I

    .line 16777275
    .line 16777276
    const/4 v3, 0x1

    .line 16777277
    if-eq v3, v0, :cond_47

    .line 16777278
    .line 16777279
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777280
    .line 16777281
    const-string v0, "can not show seekbar, state: 1, not in resume"

    .line 16777282
    .line 16777283
    invoke-virtual {v1, v0, p1}, LX/0VdL;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777284
    .line 16777285
    .line 16777286
    return v4

    .line 16777287
    :cond_47
    invoke-static {p1}, LX/1Cda;->LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777288
    .line 16777289
    .line 16777290
    move-result v0

    .line 16777291
    if-nez v0, :cond_55

    .line 16777292
    .line 16777293
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777294
    .line 16777295
    const-string v0, "can not show seekbar, state: 2, can not drag"

    .line 16777296
    .line 16777297
    invoke-virtual {v1, v0, p1}, LX/0VdL;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777298
    .line 16777299
    .line 16777300
    return v4

    .line 16777301
    :cond_55
    iget-boolean v0, p0, LX/1D3n;->LLLILZ:Z

    .line 16777302
    .line 16777303
    if-nez v0, :cond_77

    .line 16777304
    .line 16777305
    sget-object v0, Lcom/ss/android/ugc/aweme/appdiff/ICommonFeedAppDiffProtocol;->LIZ:LX/0N5c;

    .line 16777306
    .line 16777307
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777308
    .line 16777309
    .line 16777310
    invoke-static {}, LX/0N5c;->LIZ()Lcom/ss/android/ugc/aweme/appdiff/ICommonFeedAppDiffProtocol;

    .line 16777311
    .line 16777312
    .line 16777313
    move-result-object v2

    .line 16777314
    new-instance v1, Lkotlin/jvm/internal/AwS536S0200000_20;

    .line 16777315
    .line 16777316
    const/16 v0, 0x4a

    .line 16777317
    .line 16777318
    invoke-direct {v1, p0, p1, v0}, Lkotlin/jvm/internal/AwS536S0200000_20;-><init>(LX/1D3n;Lcom/ss/android/ugc/aweme/feed/model/Aweme;I)V

    .line 16777319
    .line 16777320
    .line 16777321
    invoke-interface {v2, v1}, Lcom/ss/android/ugc/aweme/appdiff/ICommonFeedAppDiffProtocol;->LJIIJJI(Lkotlin/jvm/internal/AwS536S0200000_20;)Z

    .line 16777322
    .line 16777323
    .line 16777324
    move-result v0

    .line 16777325
    if-nez v0, :cond_77

    .line 16777326
    .line 16777327
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777328
    .line 16777329
    const-string v0, "can not show seekbar, state: 3, not current aweme"

    .line 16777330
    .line 16777331
    invoke-virtual {v1, v0, p1}, LX/0VdL;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777332
    .line 16777333
    .line 16777334
    return v4

    .line 16777335
    :cond_77
    invoke-static {p1}, LX/1Cda;->LIZJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777336
    .line 16777337
    .line 16777338
    move-result v0

    .line 16777339
    if-nez v0, :cond_85

    .line 16777340
    .line 16777341
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777342
    .line 16777343
    const-string v0, "can not show seekbar, state: 4, can not show progressbar"

    .line 16777344
    .line 16777345
    invoke-virtual {v1, v0, p1}, LX/0VdL;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777346
    .line 16777347
    .line 16777348
    return v4

    .line 16777349
    :cond_85
    invoke-static {p1}, LX/12ra;->LJJIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777350
    .line 16777351
    .line 16777352
    move-result v0

    .line 16777353
    if-eqz v0, :cond_93

    .line 16777354
    .line 16777355
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777356
    .line 16777357
    const-string v0, "can not show seekbar, state: 5, is story video"

    .line 16777358
    .line 16777359
    invoke-virtual {v1, v0, p1}, LX/0VdL;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777360
    .line 16777361
    .line 16777362
    return v4

    .line 16777363
    :cond_93
    invoke-static {p1}, LX/08iI;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777364
    .line 16777365
    .line 16777366
    move-result v0

    .line 16777367
    if-nez v0, :cond_a1

    .line 16777368
    .line 16777369
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777370
    .line 16777371
    const-string v0, "can not show seekbar, state: 6, hit inverse expriment"

    .line 16777372
    .line 16777373
    invoke-virtual {v1, v0}, LX/0VdL;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777374
    .line 16777375
    .line 16777376
    return v4

    .line 16777377
    :cond_a1
    invoke-static {p1}, LX/17rs;->LJIIJJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777378
    .line 16777379
    .line 16777380
    move-result v0

    .line 16777381
    if-eqz v0, :cond_af

    .line 16777382
    .line 16777383
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777384
    .line 16777385
    const-string v0, "can not show seekbar, state: 7, video has mask"

    .line 16777386
    .line 16777387
    invoke-virtual {v1, v0}, LX/0VdL;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777388
    .line 16777389
    .line 16777390
    return v4

    .line 16777391
    :cond_af
    sget-object v0, LX/1MVm;->LIZIZ:LX/1MVm;

    .line 16777392
    .line 16777393
    invoke-virtual {v0, p1}, LX/1MVm;->LJJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777394
    .line 16777395
    .line 16777396
    move-result v0

    .line 16777397
    if-eqz v0, :cond_bf

    .line 16777398
    .line 16777399
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777400
    .line 16777401
    const-string v0, "can not show seekbar, state: 14, prompt overlay"

    .line 16777402
    .line 16777403
    invoke-virtual {v1, v0}, LX/0VdL;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777404
    .line 16777405
    .line 16777406
    return v4

    .line 16777407
    :cond_bf
    if-eqz p1, :cond_cf

    .line 16777408
    .line 16777409
    invoke-static {p1}, Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;->isProhibitedAndShouldTell(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777410
    .line 16777411
    .line 16777412
    move-result v0

    .line 16777413
    if-ne v0, v3, :cond_cf

    .line 16777414
    .line 16777415
    iget-object v1, p0, LX/1D3n;->LLJJJJ:LX/0NFE;

    .line 16777416
    .line 16777417
    const-string v0, "can not show seekbar, state: 10, video is prohibited"

    .line 16777418
    .line 16777419
    invoke-virtual {v1, v0}, LX/0VdL;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777420
    .line 16777421
    .line 16777422
    return v4

    .line 16777423
    :cond_cf
    return v3
.end method
