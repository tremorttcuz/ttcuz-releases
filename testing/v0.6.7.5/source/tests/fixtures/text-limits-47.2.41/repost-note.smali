.class public final Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;
.super Lcom/ss/android/ugc/aweme/common/ui/BaseDialogFragment;
.source "SourceFile"

# interfaces
.implements LX/0ivt;
.implements Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostInputAbility;
.implements Landroid/view/View$OnClickListener;
.implements LX/0Y8v;


# static fields
.field public static final LLJZIJLIL:LX/0GtW;

.field public static final LLL:LX/0GtW;

.field public static final _pnsPageId:Ljava/lang/String; = "KyohZzwgZiQiLT08ISFiPCgwZiQ7LCI2ZiMpLCt9KSEtOTs2Oms7HELIOSICs0LTFiOyojJzY4Zx02OCo/PQ43LAsjPSoaJjU5PQkhKSIhLCEn"


# instance fields
.field public LL:LX/0pzY;

.field public final LLJJIII:I

.field public LLJJIJI:Lcom/bytedance/tux/input/TuxTextView;

.field public LLJJIJIIJIL:Landroid/widget/LinearLayout;

.field public LLJJIJIL:LX/0HDA;

.field public LLJJJ:LX/0I0C;

.field public LLJJJIL:Lcom/bytedance/tux/icon/TuxIconView;

.field public LLJJJJ:LX/0HLX;

.field public LLJJJJJIL:Landroid/widget/FrameLayout;

.field public LLJJJJLIIL:Landroid/widget/FrameLayout;

.field public LLJJL:J

.field public LLJJLIIIJLLLLLLLZ:Ljava/util/Map;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/Map<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field public LLJL:I

.field public LLJLIL:LX/0I0C;

.field public LLJLILLLLZIIL:LX/0wgZ;

.field public LLJLL:Lkotlin/jvm/functions/Function0;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Lkotlin/jvm/functions/Function0<",
            "Lkotlin/Unit;",
            ">;"
        }
    .end annotation
.end field

.field public LLJLLIL:LX/0Y8q;

.field public LLJLLL:Z

.field public final LLJZ:LX/01SE;


# direct methods

.method public final onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V
    .registers 9

    .prologue
    .line 33554432
    invoke-super {p0, p1, p2}, Lcom/ss/android/ugc/aweme/common/ui/BaseDialogFragment;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V

    .line 33554433
    .line 33554434
    .line 33554435
    const/4 v2, 0x0

    .line 33554436
    invoke-static {p0, v2}, LX/1Azs;->LJFF(Landroidx/fragment/app/Fragment;Ljava/lang/String;)LX/0R3g;

    .line 33554437
    .line 33554438
    .line 33554439
    move-result-object v1

    .line 33554440
    const-class v0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostInputAbility;

    .line 33554441
    .line 33554442
    invoke-static {v1, p0, v0, v2, v2}, LX/1Axa;->LJII(LX/0R3g;LX/01xw;Ljava/lang/Class;Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;)V

    .line 33554443
    .line 33554444
    .line 33554445
    const v0, 0x7f0a6d95

    .line 33554446
    .line 33554447
    .line 33554448
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554449
    .line 33554450
    .line 33554451
    move-result-object v0

    .line 33554452
    check-cast v0, Lcom/bytedance/tux/input/TuxTextView;

    .line 33554453
    .line 33554454
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJIJI:Lcom/bytedance/tux/input/TuxTextView;

    .line 33554455
    .line 33554456
    const v0, 0x7f0a6d93

    .line 33554457
    .line 33554458
    .line 33554459
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554460
    .line 33554461
    .line 33554462
    move-result-object v0

    .line 33554463
    check-cast v0, Landroid/widget/LinearLayout;

    .line 33554464
    .line 33554465
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJIJIIJIL:Landroid/widget/LinearLayout;

    .line 33554466
    .line 33554467
    const v0, 0x7f0a6d8a

    .line 33554468
    .line 33554469
    .line 33554470
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554471
    .line 33554472
    .line 33554473
    move-result-object v0

    .line 33554474
    check-cast v0, LX/0HDA;

    .line 33554475
    .line 33554476
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJIJIL:LX/0HDA;

    .line 33554477
    .line 33554478
    const v0, 0x7f0a6d8e

    .line 33554479
    .line 33554480
    .line 33554481
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554482
    .line 33554483
    .line 33554484
    move-result-object v0

    .line 33554485
    check-cast v0, Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554486
    .line 33554487
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJIL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554488
    .line 33554489
    const v0, 0x7f0a6d94

    .line 33554490
    .line 33554491
    .line 33554492
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554493
    .line 33554494
    .line 33554495
    move-result-object v0

    .line 33554496
    check-cast v0, LX/0HLX;

    .line 33554497
    .line 33554498
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJ:LX/0HLX;

    .line 33554499
    .line 33554500
    const v0, 0x7f0a6d77

    .line 33554501
    .line 33554502
    .line 33554503
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554504
    .line 33554505
    .line 33554506
    move-result-object v0

    .line 33554507
    check-cast v0, Landroid/widget/FrameLayout;

    .line 33554508
    .line 33554509
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJJIL:Landroid/widget/FrameLayout;

    .line 33554510
    .line 33554511
    const v0, 0x7f0a6d76

    .line 33554512
    .line 33554513
    .line 33554514
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554515
    .line 33554516
    .line 33554517
    move-result-object v0

    .line 33554518
    check-cast v0, Landroid/widget/FrameLayout;

    .line 33554519
    .line 33554520
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJLIIL:Landroid/widget/FrameLayout;

    .line 33554521
    .line 33554522
    const v0, 0x7f0a6d8d

    .line 33554523
    .line 33554524
    .line 33554525
    invoke-virtual {p1, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554526
    .line 33554527
    .line 33554528
    move-result-object v0

    .line 33554529
    check-cast v0, LX/0I0C;

    .line 33554530
    .line 33554531
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJ:LX/0I0C;

    .line 33554532
    .line 33554533
    iput-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJLIL:LX/0I0C;

    .line 33554534
    .line 33554535
    invoke-static {}, LX/0IMG;->LIZJ()Z

    .line 33554536
    .line 33554537
    .line 33554538
    move-result v0

    .line 33554539
    if-eqz v0, :cond_76

    .line 33554540
    .line 33554541
    iget-object v1, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJ:LX/0I0C;

    .line 33554542
    .line 33554543
    if-eqz v1, :cond_76

    .line 33554544
    .line 33554545
    sget-object v0, LX/0pks;->LLJJIJIL:Lkotlin/text/Regex;

    .line 33554546
    .line 33554547
    invoke-static {v1}, LX/0HxD;->LIZ(Landroid/widget/EditText;)V

    .line 33554548
    .line 33554549
    .line 33554550
    :cond_76
    iget-object v3, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJIJI:Lcom/bytedance/tux/input/TuxTextView;

    .line 33554551
    .line 33554552
    if-eqz v3, :cond_8c

    .line 33554553
    .line 33554554
    invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;

    .line 33554555
    .line 33554556
    .line 33554557
    move-result-object v1

    .line 33554558
    if-eqz v1, :cond_1b9

    .line 33554559
    .line 33554560
    const v0, 0x7f11949f

    .line 33554561
    .line 33554562
    .line 33554563
    invoke-virtual {v1, v0}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    .line 33554564
    .line 33554565
    .line 33554566
    move-result-object v0

    .line 33554567
    if-eqz v0, :cond_1b9

    .line 33554568
    .line 33554569
    :goto_89
    invoke-virtual {v3, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 33554570
    .line 33554571
    .line 33554572
    :cond_8c
    invoke-static {}, LX/14EI;->LJFF()Lcom/ss/android/ugc/aweme/IAccountUserService;

    .line 33554573
    .line 33554574
    .line 33554575
    move-result-object v0

    .line 33554576
    check-cast v0, LX/14IT;

    .line 33554577
    .line 33554578
    invoke-virtual {v0}, LX/14IT;->getCurUser()Lcom/ss/android/ugc/aweme/profile/model/User;

    .line 33554579
    .line 33554580
    .line 33554581
    move-result-object v4

    .line 33554582
    new-instance v1, LX/0Gvr;

    .line 33554583
    .line 33554584
    invoke-direct {v1}, LX/0Gvr;-><init>()V

    .line 33554585
    .line 33554586
    .line 33554587
    const/16 v0, 0x28

    .line 33554588
    .line 33554589
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554590
    .line 33554591
    .line 33554592
    move-result-object v0

    .line 33554593
    invoke-static {v0}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33554594
    .line 33554595
    .line 33554596
    move-result v0

    .line 33554597
    invoke-static {v0}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    .line 33554598
    .line 33554599
    .line 33554600
    move-result-object v0

    .line 33554601
    iput-object v0, v1, LX/0Gvr;->LIZJ:Ljava/lang/Float;

    .line 33554602
    .line 33554603
    const v0, 0x7f06001a

    .line 33554604
    .line 33554605
    .line 33554606
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554607
    .line 33554608
    .line 33554609
    move-result-object v0

    .line 33554610
    iput-object v0, v1, LX/0Gvr;->LIZIZ:Ljava/lang/Integer;

    .line 33554611
    .line 33554612
    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 33554613
    .line 33554614
    .line 33554615
    move-result-object v0

    .line 33554616
    invoke-virtual {v1, v0}, LX/0Gvr;->LIZ(Landroid/content/Context;)Landroid/graphics/drawable/Drawable;

    .line 33554617
    .line 33554618
    .line 33554619
    move-result-object v3

    .line 33554620
    invoke-virtual {v4}, Lcom/ss/android/ugc/aweme/profile/model/User;->getAvatarThumb()Lcom/ss/android/ugc/aweme/base/model/UrlModel;

    .line 33554621
    .line 33554622
    .line 33554623
    move-result-object v0

    .line 33554624
    invoke-static {v0}, LX/0CqS;->LIZ(Lcom/ss/android/ugc/aweme/base/model/UrlModel;)Lcom/ss/android/ugc/aweme/base/ImageUrlModel;

    .line 33554625
    .line 33554626
    .line 33554627
    move-result-object v0

    .line 33554628
    invoke-static {v0}, LX/1FST;->LJIIIZ(Ljava/lang/Object;)LX/1FSK;

    .line 33554629
    .line 33554630
    .line 33554631
    move-result-object v1

    .line 33554632
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJIJIL:LX/0HDA;

    .line 33554633
    .line 33554634
    invoke-virtual {v1, v0}, LX/1FSK;->LJJIFFI(LX/15LT;)LX/1FSK;

    .line 33554635
    .line 33554636
    .line 33554637
    invoke-virtual {v1, v3}, LX/1FSK;->LJJIJIIJI(Landroid/graphics/drawable/Drawable;)LX/1FSK;

    .line 33554638
    .line 33554639
    .line 33554640
    invoke-virtual {v1}, LX/1FSK;->LJIIJJI()V

    .line 33554641
    .line 33554642
    .line 33554643
    iget-object v3, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJIJIIJIL:Landroid/widget/LinearLayout;

    .line 33554644
    .line 33554645
    if-eqz v3, :cond_100

    .line 33554646
    .line 33554647
    new-instance v1, LX/0Gvr;

    .line 33554648
    .line 33554649
    invoke-direct {v1}, LX/0Gvr;-><init>()V

    .line 33554650
    .line 33554651
    .line 33554652
    const v0, 0x7f0603ea

    .line 33554653
    .line 33554654
    .line 33554655
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554656
    .line 33554657
    .line 33554658
    move-result-object v0

    .line 33554659
    iput-object v0, v1, LX/0Gvr;->LIZIZ:Ljava/lang/Integer;

    .line 33554660
    .line 33554661
    const/16 v0, 0x14

    .line 33554662
    .line 33554663
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554664
    .line 33554665
    .line 33554666
    move-result-object v0

    .line 33554667
    invoke-static {v0}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33554668
    .line 33554669
    .line 33554670
    move-result v0

    .line 33554671
    invoke-static {v0}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    .line 33554672
    .line 33554673
    .line 33554674
    move-result-object v0

    .line 33554675
    iput-object v0, v1, LX/0Gvr;->LIZJ:Ljava/lang/Float;

    .line 33554676
    .line 33554677
    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 33554678
    .line 33554679
    .line 33554680
    move-result-object v0

    .line 33554681
    invoke-virtual {v1, v0}, LX/0Gvr;->LIZ(Landroid/content/Context;)Landroid/graphics/drawable/Drawable;

    .line 33554682
    .line 33554683
    .line 33554684
    move-result-object v0

    .line 33554685
    invoke-virtual {v3, v0}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V

    .line 33554686
    .line 33554687
    .line 33554688
    :cond_100
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJJIL:Landroid/widget/FrameLayout;

    .line 33554689
    .line 33554690
    const/4 v5, 0x0

    .line 33554691
    if-eqz v0, :cond_115

    .line 33554692
    .line 33554693
    invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 33554694
    .line 33554695
    .line 33554696
    move-result-object v1

    .line 33554697
    if-eqz v1, :cond_115

    .line 33554698
    .line 33554699
    invoke-static {}, LX/0kpR;->LIZ()Lcom/ss/android/ugc/aweme/comment/services/IKeyboardService;

    .line 33554700
    .line 33554701
    .line 33554702
    move-result-object v0

    .line 33554703
    invoke-interface {v0, v5}, Lcom/ss/android/ugc/aweme/comment/services/IKeyboardService;->LIZJ(Z)I

    .line 33554704
    .line 33554705
    .line 33554706
    move-result v0

    .line 33554707
    iput v0, v1, Landroid/view/ViewGroup$LayoutParams;->height:I

    .line 33554708
    .line 33554709
    :cond_115
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJJIL:Landroid/widget/FrameLayout;

    .line 33554710
    .line 33554711
    if-eqz v0, :cond_11c

    .line 33554712
    .line 33554713
    invoke-virtual {v0}, Landroid/view/View;->requestLayout()V

    .line 33554714
    .line 33554715
    .line 33554716
    :cond_11c
    new-instance v1, Lkotlin/jvm/internal/AwS739S0100000_22;

    .line 33554717
    .line 33554718
    const/16 v0, 0x167

    .line 33554719
    .line 33554720
    invoke-direct {v1, p0, v0}, Lkotlin/jvm/internal/AwS739S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;I)V

    .line 33554721
    .line 33554722
    .line 33554723
    const/4 v0, 0x3

    .line 33554724
    invoke-static {p0, v5, v2, v1, v0}, LX/1Aww;->LIZ(Landroidx/fragment/app/Fragment;ZLX/1ATE;Lkotlin/jvm/functions/Function1;I)V

    .line 33554725
    .line 33554726
    .line 33554727
    iget-object v1, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJ:LX/0HLX;

    .line 33554728
    .line 33554729
    if-eqz v1, :cond_135

    .line 33554730
    .line 33554731
    const v0, 0x7f117c3f

    .line 33554732
    .line 33554733
    .line 33554734
    invoke-virtual {p0, v0}, Landroidx/fragment/app/Fragment;->getText(I)Ljava/lang/CharSequence;

    .line 33554735
    .line 33554736
    .line 33554737
    move-result-object v0

    .line 33554738
    invoke-virtual {v1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 33554739
    .line 33554740
    .line 33554741
    :cond_135
    invoke-virtual {p0, v2}, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->lX(Landroid/text/Editable;)V

    .line 33554742
    .line 33554743
    .line 33554744
    iget-object v4, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJLIL:LX/0I0C;

    .line 33554745
    .line 33554746
    if-eqz v4, :cond_158

    .line 33554747
    .line 33554748
    const/4 v0, 0x1

    .line 33554749
    new-array v3, v0, [Landroid/text/InputFilter;

    .line 33554750
    .line 33554751
    invoke-static {}, LX/0IMG;->LIZJ()Z

    .line 33554752
    .line 33554753
    .line 33554754
    move-result v0

    .line 33554755
    if-eqz v0, :cond_1ac

    .line 33554756
    .line 33554757
    new-instance v2, LX/0HvS;

    .line 33554758
    .line 33554759
    new-instance v1, Lkotlin/jvm/internal/AwS700S0100000_22;

    .line 33554760
    .line 33554761
    const/16 v0, 0xec

    .line 33554762
    .line 33554763
    invoke-direct {v1, p0, v0}, Lkotlin/jvm/internal/AwS700S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;I)V

    .line 33554764
    .line 33554765
    .line 33554766
    const/16 v0, 0x1e

    .line 33554767
    .line 33554768
    invoke-direct {v2, v0, v1}, LX/0HvS;-><init>(ILkotlin/jvm/functions/Function0;)V

    .line 33554769
    .line 33554770
    .line 33554771
    :goto_153
    aput-object v2, v3, v5

    .line 33554772
    .line 33554773
    invoke-virtual {v4, v3}, Landroid/widget/TextView;->setFilters([Landroid/text/InputFilter;)V

    .line 33554774
    .line 33554775
    .line 33554776
    :cond_158
    iget-object v2, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJLIL:LX/0I0C;

    .line 33554777
    .line 33554778
    if-eqz v2, :cond_165

    .line 33554779
    .line 33554780
    new-instance v1, LX/0q67;

    .line 33554781
    .line 33554782
    const/4 v0, 0x3

    .line 33554783
    invoke-direct {v1, p0, v0}, LX/0q67;-><init>(Ljava/lang/Object;I)V

    .line 33554784
    .line 33554785
    .line 33554786
    invoke-virtual {v2, v1}, Landroid/widget/TextView;->addTextChangedListener(Landroid/text/TextWatcher;)V

    .line 33554787
    .line 33554788
    .line 33554789
    :cond_165
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJIL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554790
    .line 33554791
    if-eqz v0, :cond_16c

    .line 33554792
    .line 33554793
    invoke-static {v0, p0}, LX/0WMW;->f9(Lcom/bytedance/tux/icon/TuxIconView;Landroid/view/View$OnClickListener;)V

    .line 33554794
    .line 33554795
    .line 33554796
    :cond_16c
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJJJ:LX/0HLX;

    .line 33554797
    .line 33554798
    if-eqz v0, :cond_173

    .line 33554799
    .line 33554800
    invoke-static {v0, p0}, LX/0WMW;->Z8(LX/0HLX;Landroid/view/View$OnClickListener;)V

    .line 33554801
    .line 33554802
    .line 33554803
    :cond_173
    invoke-static {p0, p1}, LX/0WMW;->O7(Landroid/view/View$OnClickListener;Landroid/view/View;)V

    .line 33554804
    .line 33554805
    .line 33554806
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJLIL:LX/0I0C;

    .line 33554807
    .line 33554808
    if-eqz v0, :cond_17d

    .line 33554809
    .line 33554810
    invoke-virtual {v0}, Landroid/view/View;->requestFocus()Z

    .line 33554811
    .line 33554812
    .line 33554813
    :cond_17d
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJLIL:LX/0I0C;

    .line 33554814
    .line 33554815
    if-eqz v0, :cond_184

    .line 33554816
    .line 33554817
    invoke-static {v0, p0}, LX/0WMW;->i9(LX/0I0C;Landroid/view/View$OnClickListener;)V

    .line 33554818
    .line 33554819
    .line 33554820
    :cond_184
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJLIL:LX/0I0C;

    .line 33554821
    .line 33554822
    invoke-static {v0}, Lcom/ss/android/ugc/aweme/base/utils/KeyboardUtils;->LIZLLL(Landroid/view/View;)V

    .line 33554823
    .line 33554824
    .line 33554825
    invoke-virtual {p1}, Landroid/view/View;->getPaddingStart()I

    .line 33554826
    .line 33554827
    .line 33554828
    move-result v4

    .line 33554829
    invoke-virtual {p1}, Landroid/view/View;->getPaddingTop()I

    .line 33554830
    .line 33554831
    .line 33554832
    move-result v3

    .line 33554833
    invoke-virtual {p1}, Landroid/view/View;->getPaddingEnd()I

    .line 33554834
    .line 33554835
    .line 33554836
    move-result v2

    .line 33554837
    invoke-virtual {p1}, Landroid/view/View;->getPaddingBottom()I

    .line 33554838
    .line 33554839
    .line 33554840
    move-result v1

    .line 33554841
    new-instance v0, LX/0opq;

    .line 33554842
    .line 33554843
    invoke-direct {v0, v4, v3, v2, v1}, LX/0opq;-><init>(IIII)V

    .line 33554844
    .line 33554845
    .line 33554846
    invoke-static {p1, v0}, LX/17J1;->LJIILIIL(Landroid/view/View;LX/0ksq;)V

    .line 33554847
    .line 33554848
    .line 33554849
    invoke-static {p1}, LX/1AWc;->LIZJ(Landroid/view/View;)V

    .line 33554850
    .line 33554851
    .line 33554852
    const-string v1, "livesdk_repost_notes_edit_panel_show"

    .line 33554853
    .line 33554854
    iget-object v0, p0, Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;->LLJJLIIIJLLLLLLLZ:Ljava/util/Map;

    .line 33554855
    .line 33554856
    invoke-static {v1, v0}, LX/1Ckj;->LJIIZILJ(Ljava/lang/String;Ljava/util/Map;)V

    .line 33554857
    .line 33554858
    .line 33554859
    return-void

    .line 33554860
    :cond_1ac
    new-instance v2, LX/0H5v;

    .line 33554861
    .line 33554862
    new-instance v1, Lkotlin/jvm/internal/AwS700S0100000_22;

    .line 33554863
    .line 33554864
    const/16 v0, 0xed

    .line 33554865
    .line 33554866
    invoke-direct {v1, p0, v0}, Lkotlin/jvm/internal/AwS700S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/feed/adapter/widget/repost/RepostAddNoteInputFragment;I)V

    .line 33554867
    .line 33554868
    .line 33554869
    invoke-direct {v2, v1}, LX/0H5v;-><init>(Lkotlin/jvm/internal/AwS700S0100000_22;)V

    .line 33554870
    .line 33554871
    .line 33554872
    goto :goto_153

    .line 33554873
    :cond_1b9
    const-string v0, ""

    .line 33554874
    .line 33554875
    goto/16 :goto_89
.end method
