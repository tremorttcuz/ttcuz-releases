.class public final LX/0oq2;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements LX/00kC;


# instance fields
.field public final LL:Ljava/lang/String;

.field public final LLJJIII:Ljava/lang/String;

.field public final LLJJIJI:Ljava/lang/String;

.field public final LLJJIJIIJIL:Ljava/lang/Integer;

.field public final LLJJIJIL:Z

.field public final LLJJJ:I

.field public final LLJJJIL:I

.field public final LLJJJJ:Ljava/lang/String;

.field public final LLJJJJJIL:I

.field public final LLJJJJLIIL:Lkotlin/jvm/functions/Function0;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;"
        }
    .end annotation
.end field


# direct methods

.method public constructor <init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;ZIILjava/lang/String;ILkotlin/jvm/functions/Function0;I)V
    .registers 14

    .prologue
    .line 184549376
    and-int/lit8 v0, p11, 0x2

    .line 184549377
    .line 184549378
    const/4 v1, 0x0

    .line 184549379
    if-eqz v0, :cond_6

    .line 184549380
    .line 184549381
    move-object p2, v1

    .line 184549382
    :cond_6
    and-int/lit8 v0, p11, 0x4

    .line 184549383
    .line 184549384
    if-eqz v0, :cond_b

    .line 184549385
    .line 184549386
    move-object p3, v1

    .line 184549387
    :cond_b
    and-int/lit8 v0, p11, 0x8

    .line 184549388
    .line 184549389
    if-eqz v0, :cond_10

    .line 184549390
    .line 184549391
    move-object p4, v1

    .line 184549392
    :cond_10
    and-int/lit8 v0, p11, 0x10

    .line 184549393
    .line 184549394
    if-eqz v0, :cond_15

    .line 184549395
    .line 184549396
    const/4 p5, 0x0

    .line 184549397
    :cond_15
    and-int/lit8 v0, p11, 0x20

    .line 184549398
    .line 184549399
    if-eqz v0, :cond_1c

    .line 184549400
    .line 184549401
    const p6, 0x7f0a6da4

    .line 184549402
    .line 184549403
    .line 184549404
    :cond_1c
    and-int/lit8 v0, p11, 0x40

    .line 184549405
    .line 184549406
    if-eqz v0, :cond_21

    .line 184549407
    .line 184549408
    const/4 p7, 0x0

    .line 184549409
    :cond_21
    and-int/lit16 v0, p11, 0x80

    .line 184549410
    .line 184549411
    if-eqz v0, :cond_26

    .line 184549412
    .line 184549413
    move-object p8, v1

    .line 184549414
    :cond_26
    and-int/lit16 v0, p11, 0x100

    .line 184549415
    .line 184549416
    if-eqz v0, :cond_38

    .line 184549417
    .line 184549418
    sget-object v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->_pnsPageId:Ljava/lang/String;

    .line 184549419
    .line 184549420
    sget-object v0, LX/08uW;->LIZ:LX/01SE;

    .line 184549421
    .line 184549422
    invoke-interface {v0}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 184549423
    .line 184549424
    .line 184549425
    move-result-object v0

    .line 184549426
    check-cast v0, Ljava/lang/Number;

    .line 184549427
    .line 184549428
    invoke-virtual {v0}, Ljava/lang/Number;->intValue()I

    .line 184549429
    .line 184549430
    .line 184549431
    move-result p9

    .line 184549432
    :cond_38
    and-int/lit16 v0, p11, 0x200

    .line 184549433
    .line 184549434
    if-eqz v0, :cond_3d

    .line 184549435
    .line 184549436
    move-object p10, v1

    .line 184549437
    :cond_3d
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 184549438
    .line 184549439
    .line 184549440
    iput-object p1, p0, LX/0oq2;->LL:Ljava/lang/String;

    .line 184549441
    .line 184549442
    iput-object p2, p0, LX/0oq2;->LLJJIII:Ljava/lang/String;

    .line 184549443
    .line 184549444
    iput-object p3, p0, LX/0oq2;->LLJJIJI:Ljava/lang/String;

    .line 184549445
    .line 184549446
    iput-object p4, p0, LX/0oq2;->LLJJIJIIJIL:Ljava/lang/Integer;

    .line 184549447
    .line 184549448
    iput-boolean p5, p0, LX/0oq2;->LLJJIJIL:Z

    .line 184549449
    .line 184549450
    iput p6, p0, LX/0oq2;->LLJJJ:I

    .line 184549451
    .line 184549452
    iput p7, p0, LX/0oq2;->LLJJJIL:I

    .line 184549453
    .line 184549454
    iput-object p8, p0, LX/0oq2;->LLJJJJ:Ljava/lang/String;

    .line 184549455
    .line 184549456
    iput p9, p0, LX/0oq2;->LLJJJJJIL:I

    .line 184549457
    .line 184549458
    iput-object p10, p0, LX/0oq2;->LLJJJJLIIL:Lkotlin/jvm/functions/Function0;

    .line 184549459
    .line 184549460
    return-void
.end method
