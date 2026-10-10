.class public final LX/0H5v;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Landroid/text/InputFilter;


# instance fields
.field public final LL:Lkotlin/jvm/functions/Function0;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;"
        }
    .end annotation
.end field


# direct methods

.method public final filter(Ljava/lang/CharSequence;IILandroid/text/Spanned;II)Ljava/lang/CharSequence;
    .registers 11

    .prologue
    .line 100663296
    invoke-interface {p4}, Landroid/text/Spanned;->length()I

    .line 100663297
    .line 100663298
    .line 100663299
    move-result v0

    .line 100663300
    sub-int/2addr p6, p5

    .line 100663301
    sub-int/2addr v0, p6

    .line 100663302
    rsub-int/lit8 v3, v0, 0x1e

    .line 100663303
    .line 100663304
    const-string v2, ""

    .line 100663305
    .line 100663306
    if-gtz v3, :cond_1a

    .line 100663307
    .line 100663308
    invoke-interface {p1}, Ljava/lang/CharSequence;->length()I

    .line 100663309
    .line 100663310
    .line 100663311
    move-result v0

    .line 100663312
    if-lez v0, :cond_19

    .line 100663313
    .line 100663314
    iget-object v0, p0, LX/0H5v;->LL:Lkotlin/jvm/functions/Function0;

    .line 100663315
    .line 100663316
    if-eqz v0, :cond_19

    .line 100663317
    .line 100663318
    invoke-interface {v0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 100663319
    .line 100663320
    .line 100663321
    :cond_19
    return-object v2

    .line 100663322
    :cond_1a
    sub-int/2addr p3, p2

    .line 100663323
    if-lt v3, p3, :cond_1f

    .line 100663324
    .line 100663325
    const/4 v2, 0x0

    .line 100663326
    return-object v2

    .line 100663327
    :cond_1f
    add-int/2addr v3, p2

    .line 100663328
    add-int/lit8 v1, v3, -0x1

    .line 100663329
    .line 100663330
    invoke-interface {p1, v1}, Ljava/lang/CharSequence;->charAt(I)C

    .line 100663331
    .line 100663332
    .line 100663333
    move-result v0

    .line 100663334
    invoke-static {v0}, Ljava/lang/Character;->isHighSurrogate(C)Z

    .line 100663335
    .line 100663336
    .line 100663337
    move-result v0

    .line 100663338
    if-eqz v0, :cond_37

    .line 100663339
    .line 100663340
    if-ne v1, p2, :cond_36

    .line 100663341
    .line 100663342
    iget-object v0, p0, LX/0H5v;->LL:Lkotlin/jvm/functions/Function0;

    .line 100663343
    .line 100663344
    if-eqz v0, :cond_35

    .line 100663345
    .line 100663346
    invoke-interface {v0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 100663347
    .line 100663348
    .line 100663349
    :cond_35
    return-object v2

    .line 100663350
    :cond_36
    move v3, v1

    .line 100663351
    :cond_37
    iget-object v0, p0, LX/0H5v;->LL:Lkotlin/jvm/functions/Function0;

    .line 100663352
    .line 100663353
    if-eqz v0, :cond_3e

    .line 100663354
    .line 100663355
    invoke-interface {v0}, Lkotlin/jvm/functions/Function0;->invoke()Ljava/lang/Object;

    .line 100663356
    .line 100663357
    .line 100663358
    :cond_3e
    invoke-interface {p1, p2, v3}, Ljava/lang/CharSequence;->subSequence(II)Ljava/lang/CharSequence;

    .line 100663359
    .line 100663360
    .line 100663361
    move-result-object v2

    .line 100663362
    return-object v2
.end method
