.class public final LX/1Cda;
.super Ljava/lang/Object;

.method public static final LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z
    .registers 4

    .prologue
    .line 16777216
    const/4 v2, 0x0

    .line 16777217
    if-eqz p0, :cond_6f

    .line 16777218
    .line 16777219
    invoke-static {p0}, LX/1KWp;->LJFF(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777220
    .line 16777221
    .line 16777222
    move-result v0

    .line 16777223
    if-nez v0, :cond_6f

    .line 16777224
    .line 16777225
    invoke-static {p0}, LX/1CSD;->LJII(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777226
    .line 16777227
    .line 16777228
    move-result v0

    .line 16777229
    if-nez v0, :cond_6f

    .line 16777230
    .line 16777231
    invoke-static {p0}, LX/1CSD;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    if-nez v0, :cond_6f

    .line 16777236
    .line 16777237
    invoke-static {p0}, LX/1CSD;->LJIIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

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
    invoke-interface {v0, p0}, Lcom/ss/android/ugc/aweme/commercialize/promote/interfaces/ICommercePromoteService;->LJJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

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
    sget-object v0, LX/0dlN;->LIZ:LX/0dlN;

    .line 16777280
    .line 16777281
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777282
    .line 16777283
    .line 16777284
    invoke-static {p0}, LX/0dlN;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777285
    .line 16777286
    .line 16777287
    move-result v0

    .line 16777288
    if-nez v0, :cond_6f

    .line 16777289
    .line 16777290
    sget-object v1, LX/07It;->LIZ:LX/01SE;

    .line 16777291
    .line 16777292
    invoke-interface {v1}, LX/01SE;->getValue()Ljava/lang/Object;

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
    invoke-interface {v1}, LX/01SE;->getValue()Ljava/lang/Object;

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
