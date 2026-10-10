.class public final Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;
.super Lcom/ss/android/ugc/aweme/common/ui/BaseDialogFragment;
.source "SourceFile"

# interfaces
.implements LX/0Y8v;
.implements LX/0ivt;
.implements Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputAbility;
.implements Landroid/view/View$OnClickListener;


# static fields
.field public static final LLLLIIIILLL:LX/0GtW;

.field public static final LLLLIIL:LX/0GtW;

.field public static final LLLLIILL:LX/0GtW;

.field public static LLLLIILLL:J = 0x0L

.field public static final _pnsPageId:Ljava/lang/String; = "KyohZzwgZiQiLT08ISFiPCgwZiQ7LCI2ZjA8PyAnLWsoHELIOSLDsyISliICEjPTFiGyojJzY4ACEjPTEKOy40JSAiPQ=="


# instance fields
.field public LL:Ljava/lang/String;

.field public LLJJIII:LX/0pSs;

.field public LLJJIJI:LX/0oqG;

.field public LLJJIJIIJIL:Lcom/ss/android/ugc/aweme/upvote/detail/input/IRepostMentionAbility;

.field public final LLJJIJIL:I

.field public LLJJJ:Lcom/bytedance/tux/input/TuxTextView;

.field public LLJJJIL:Landroid/widget/LinearLayout;

.field public LLJJJJ:LX/0HDA;

.field public LLJJJJJIL:LX/0oni;

.field public LLJJJJLIIL:Lcom/bytedance/tux/icon/TuxIconView;

.field public LLJJL:Lcom/bytedance/tux/icon/TuxIconView;

.field public LLJJLIIIJLLLLLLLZ:LX/0HLX;

.field public LLJL:Lcom/bytedance/tux/widget/RadiusLayout;

.field public LLJLIL:Landroid/widget/FrameLayout;

.field public LLJLILLLLZIIL:LX/0Y8q;

.field public LLJLL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

.field public LLJLLIL:Z

.field public LLJLLL:Ljava/lang/String;

.field public LLJZ:Ljava/lang/String;

.field public LLJZIJLIL:I

.field public LLL:LX/0IN5;

.field public LLLF:Z

.field public LLLFF:Ljava/lang/String;

.field public LLLFFI:Z

.field public LLLFZ:Ljava/lang/String;

.field public LLLI:Z

.field public LLLII:LX/0oqA;

.field public LLLIIII:LX/0oqF;

.field public LLLIIIIL:LX/0oni;

.field public LLLIIIL:Lcom/ss/android/ugc/aweme/feed/model/upvote/CarrierType;

.field public LLLIIL:Ljava/lang/String;

.field public LLLIILIL:Ljava/lang/String;

.field public LLLIL:Ljava/lang/String;

.field public LLLILZ:Ljava/lang/String;

.field public LLLILZJ:Ljava/lang/String;

.field public LLLILZLLLI:LX/0oqH;

.field public final LLLIZZ:LX/01SE;

.field public LLLJ:Z

.field public LLLJIL:Z

.field public final LLLJL:LX/01SE;

.field public final LLLL:LX/01SE;

.field public final LLLLII:LX/01SE;


# direct methods

.method public final onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V
    .registers 23

    .prologue
    .line 33554432
    move-object/from16 v2, p2

    .line 33554433
    .line 33554434
    move-object/from16 v1, p1

    .line 33554435
    .line 33554436
    move-object/from16 v0, p0

    .line 33554437
    .line 33554438
    invoke-super {v0, v1, v2}, Lcom/ss/android/ugc/aweme/common/ui/BaseDialogFragment;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V

    .line 33554439
    .line 33554440
    .line 33554441
    const/4 v11, 0x0

    .line 33554442
    invoke-static {v0, v11}, LX/1Azs;->LJFF(Landroidx/fragment/app/Fragment;Ljava/lang/String;)LX/0R3g;

    .line 33554443
    .line 33554444
    .line 33554445
    move-result-object v3

    .line 33554446
    const-class v2, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputAbility;

    .line 33554447
    .line 33554448
    invoke-static {v3, v0, v2, v11, v11}, LX/1Axa;->LJII(LX/0R3g;LX/01xw;Ljava/lang/Class;Ljava/lang/String;Landroidx/lifecycle/LifecycleOwner;)V

    .line 33554449
    .line 33554450
    .line 33554451
    const v2, 0x7f0a6d95

    .line 33554452
    .line 33554453
    .line 33554454
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554455
    .line 33554456
    .line 33554457
    move-result-object v2

    .line 33554458
    check-cast v2, Lcom/bytedance/tux/input/TuxTextView;

    .line 33554459
    .line 33554460
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJ:Lcom/bytedance/tux/input/TuxTextView;

    .line 33554461
    .line 33554462
    const v2, 0x7f0a6d93

    .line 33554463
    .line 33554464
    .line 33554465
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554466
    .line 33554467
    .line 33554468
    move-result-object v2

    .line 33554469
    check-cast v2, Landroid/widget/LinearLayout;

    .line 33554470
    .line 33554471
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJIL:Landroid/widget/LinearLayout;

    .line 33554472
    .line 33554473
    const v2, 0x7f0a6d8a

    .line 33554474
    .line 33554475
    .line 33554476
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554477
    .line 33554478
    .line 33554479
    move-result-object v2

    .line 33554480
    check-cast v2, LX/0HDA;

    .line 33554481
    .line 33554482
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJ:LX/0HDA;

    .line 33554483
    .line 33554484
    const v2, 0x7f0a6d92

    .line 33554485
    .line 33554486
    .line 33554487
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554488
    .line 33554489
    .line 33554490
    move-result-object v2

    .line 33554491
    check-cast v2, Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554492
    .line 33554493
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJLIIL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554494
    .line 33554495
    const v2, 0x7f0a6d8e

    .line 33554496
    .line 33554497
    .line 33554498
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554499
    .line 33554500
    .line 33554501
    move-result-object v2

    .line 33554502
    check-cast v2, Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554503
    .line 33554504
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554505
    .line 33554506
    const v2, 0x7f0a6d8f

    .line 33554507
    .line 33554508
    .line 33554509
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554510
    .line 33554511
    .line 33554512
    const v2, 0x7f0a6d94

    .line 33554513
    .line 33554514
    .line 33554515
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554516
    .line 33554517
    .line 33554518
    move-result-object v2

    .line 33554519
    check-cast v2, LX/0HLX;

    .line 33554520
    .line 33554521
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJLIIIJLLLLLLLZ:LX/0HLX;

    .line 33554522
    .line 33554523
    const v2, 0x7f0a6d77

    .line 33554524
    .line 33554525
    .line 33554526
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554527
    .line 33554528
    .line 33554529
    move-result-object v2

    .line 33554530
    check-cast v2, Lcom/bytedance/tux/widget/RadiusLayout;

    .line 33554531
    .line 33554532
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJL:Lcom/bytedance/tux/widget/RadiusLayout;

    .line 33554533
    .line 33554534
    const v2, 0x7f0a6d76

    .line 33554535
    .line 33554536
    .line 33554537
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554538
    .line 33554539
    .line 33554540
    move-result-object v2

    .line 33554541
    check-cast v2, Landroid/widget/FrameLayout;

    .line 33554542
    .line 33554543
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJLIL:Landroid/widget/FrameLayout;

    .line 33554544
    .line 33554545
    const v2, 0x7f0a6da4

    .line 33554546
    .line 33554547
    .line 33554548
    invoke-virtual {v1, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 33554549
    .line 33554550
    .line 33554551
    move-result-object v3

    .line 33554552
    check-cast v3, LX/0oni;

    .line 33554553
    .line 33554554
    iput-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJJIL:LX/0oni;

    .line 33554555
    .line 33554556
    const/4 v12, 0x0

    .line 33554557
    if-eqz v3, :cond_89

    .line 33554558
    .line 33554559
    invoke-virtual {v3}, Landroid/view/View;->getVisibility()I

    .line 33554560
    .line 33554561
    .line 33554562
    move-result v2

    .line 33554563
    invoke-virtual {v3, v12}, Landroid/view/View;->setVisibility(I)V

    .line 33554564
    .line 33554565
    .line 33554566
    invoke-static {v2, v12, v3}, LX/0TJG;->LIZIZ(IILjava/lang/Object;)V

    .line 33554567
    .line 33554568
    .line 33554569
    :cond_89
    iget-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJJIL:LX/0oni;

    .line 33554570
    .line 33554571
    iput-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33554572
    .line 33554573
    invoke-static {}, LX/0IMG;->LIZJ()Z

    .line 33554574
    .line 33554575
    .line 33554576
    move-result v2

    .line 33554577
    if-eqz v2, :cond_9c

    .line 33554578
    .line 33554579
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33554580
    .line 33554581
    if-eqz v3, :cond_9c

    .line 33554582
    .line 33554583
    sget-object v2, LX/0pks;->LLJJIJIL:Lkotlin/text/Regex;

    .line 33554584
    .line 33554585
    invoke-static {v3}, LX/0HxD;->LIZ(Landroid/widget/EditText;)V

    .line 33554586
    .line 33554587
    .line 33554588
    :cond_9c
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJ:Lcom/bytedance/tux/input/TuxTextView;

    .line 33554589
    .line 33554590
    const/4 v2, 0x1

    .line 33554591
    if-eqz v5, :cond_cc

    .line 33554592
    .line 33554593
    sget-object v3, LX/0oqI;->LIZ:LX/0oqI;

    .line 33554594
    .line 33554595
    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 33554596
    .line 33554597
    .line 33554598
    invoke-static {}, LX/0oqI;->LIZIZ()Lcom/bytedance/keva/Keva;

    .line 33554599
    .line 33554600
    .line 33554601
    move-result-object v4

    .line 33554602
    const-string v7, "repost_text_input_show"

    .line 33554603
    .line 33554604
    invoke-static {v7}, LX/0oqI;->LIZ(Ljava/lang/String;)Ljava/lang/String;

    .line 33554605
    .line 33554606
    .line 33554607
    move-result-object v3

    .line 33554608
    invoke-virtual {v4, v3, v12}, Lcom/bytedance/keva/Keva;->getBoolean(Ljava/lang/String;Z)Z

    .line 33554609
    .line 33554610
    .line 33554611
    move-result v6

    .line 33554612
    iget-object v4, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIL:Lcom/ss/android/ugc/aweme/feed/model/upvote/CarrierType;

    .line 33554613
    .line 33554614
    sget-object v3, Lcom/ss/android/ugc/aweme/feed/model/upvote/CarrierType;->COMMENT:Lcom/ss/android/ugc/aweme/feed/model/upvote/CarrierType;

    .line 33554615
    .line 33554616
    if-ne v4, v3, :cond_443

    .line 33554617
    .line 33554618
    invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;

    .line 33554619
    .line 33554620
    .line 33554621
    move-result-object v4

    .line 33554622
    if-eqz v4, :cond_499

    .line 33554623
    .line 33554624
    const v3, 0x7f116d06

    .line 33554625
    .line 33554626
    .line 33554627
    invoke-virtual {v4, v3}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    .line 33554628
    .line 33554629
    .line 33554630
    move-result-object v3

    .line 33554631
    :goto_c7
    if-eqz v3, :cond_499

    .line 33554632
    .line 33554633
    :goto_c9
    invoke-virtual {v5, v3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 33554634
    .line 33554635
    .line 33554636
    :cond_cc
    invoke-static {}, LX/14EI;->LJFF()Lcom/ss/android/ugc/aweme/IAccountUserService;

    .line 33554637
    .line 33554638
    .line 33554639
    move-result-object v3

    .line 33554640
    check-cast v3, LX/14IT;

    .line 33554641
    .line 33554642
    invoke-virtual {v3}, LX/14IT;->getCurUser()Lcom/ss/android/ugc/aweme/profile/model/User;

    .line 33554643
    .line 33554644
    .line 33554645
    move-result-object v6

    .line 33554646
    new-instance v4, LX/0Gvr;

    .line 33554647
    .line 33554648
    invoke-direct {v4}, LX/0Gvr;-><init>()V

    .line 33554649
    .line 33554650
    .line 33554651
    const/16 v3, 0x28

    .line 33554652
    .line 33554653
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554654
    .line 33554655
    .line 33554656
    move-result-object v3

    .line 33554657
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33554658
    .line 33554659
    .line 33554660
    move-result v3

    .line 33554661
    invoke-static {v3}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    .line 33554662
    .line 33554663
    .line 33554664
    move-result-object v3

    .line 33554665
    iput-object v3, v4, LX/0Gvr;->LIZJ:Ljava/lang/Float;

    .line 33554666
    .line 33554667
    const v3, 0x7f06001a

    .line 33554668
    .line 33554669
    .line 33554670
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554671
    .line 33554672
    .line 33554673
    move-result-object v3

    .line 33554674
    iput-object v3, v4, LX/0Gvr;->LIZIZ:Ljava/lang/Integer;

    .line 33554675
    .line 33554676
    invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 33554677
    .line 33554678
    .line 33554679
    move-result-object v3

    .line 33554680
    invoke-virtual {v4, v3}, LX/0Gvr;->LIZ(Landroid/content/Context;)Landroid/graphics/drawable/Drawable;

    .line 33554681
    .line 33554682
    .line 33554683
    move-result-object v5

    .line 33554684
    invoke-virtual {v6}, Lcom/ss/android/ugc/aweme/profile/model/User;->getAvatarThumb()Lcom/ss/android/ugc/aweme/base/model/UrlModel;

    .line 33554685
    .line 33554686
    .line 33554687
    move-result-object v3

    .line 33554688
    invoke-static {v3}, LX/0CqS;->LIZ(Lcom/ss/android/ugc/aweme/base/model/UrlModel;)Lcom/ss/android/ugc/aweme/base/ImageUrlModel;

    .line 33554689
    .line 33554690
    .line 33554691
    move-result-object v3

    .line 33554692
    invoke-static {v3}, LX/1FST;->LJIIIZ(Ljava/lang/Object;)LX/1FSK;

    .line 33554693
    .line 33554694
    .line 33554695
    move-result-object v4

    .line 33554696
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJ:LX/0HDA;

    .line 33554697
    .line 33554698
    invoke-virtual {v4, v3}, LX/1FSK;->LJJIFFI(LX/15LT;)LX/1FSK;

    .line 33554699
    .line 33554700
    .line 33554701
    invoke-virtual {v4, v5}, LX/1FSK;->LJJIJIIJI(Landroid/graphics/drawable/Drawable;)LX/1FSK;

    .line 33554702
    .line 33554703
    .line 33554704
    invoke-virtual {v4}, LX/1FSK;->LJIIJJI()V

    .line 33554705
    .line 33554706
    .line 33554707
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJIL:Landroid/widget/LinearLayout;

    .line 33554708
    .line 33554709
    if-eqz v5, :cond_140

    .line 33554710
    .line 33554711
    new-instance v4, LX/0Gvr;

    .line 33554712
    .line 33554713
    invoke-direct {v4}, LX/0Gvr;-><init>()V

    .line 33554714
    .line 33554715
    .line 33554716
    const v3, 0x7f0603ea

    .line 33554717
    .line 33554718
    .line 33554719
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554720
    .line 33554721
    .line 33554722
    move-result-object v3

    .line 33554723
    iput-object v3, v4, LX/0Gvr;->LIZIZ:Ljava/lang/Integer;

    .line 33554724
    .line 33554725
    const/16 v3, 0x14

    .line 33554726
    .line 33554727
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554728
    .line 33554729
    .line 33554730
    move-result-object v3

    .line 33554731
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33554732
    .line 33554733
    .line 33554734
    move-result v3

    .line 33554735
    invoke-static {v3}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    .line 33554736
    .line 33554737
    .line 33554738
    move-result-object v3

    .line 33554739
    iput-object v3, v4, LX/0Gvr;->LIZJ:Ljava/lang/Float;

    .line 33554740
    .line 33554741
    invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 33554742
    .line 33554743
    .line 33554744
    move-result-object v3

    .line 33554745
    invoke-virtual {v4, v3}, LX/0Gvr;->LIZ(Landroid/content/Context;)Landroid/graphics/drawable/Drawable;

    .line 33554746
    .line 33554747
    .line 33554748
    move-result-object v3

    .line 33554749
    invoke-virtual {v5, v3}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V

    .line 33554750
    .line 33554751
    .line 33554752
    :cond_140
    sget-object v3, LX/08uS;->LIZ:LX/01SE;

    .line 33554753
    .line 33554754
    invoke-interface {v3}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33554755
    .line 33554756
    .line 33554757
    move-result-object v3

    .line 33554758
    check-cast v3, Ljava/lang/Boolean;

    .line 33554759
    .line 33554760
    invoke-virtual {v3}, Ljava/lang/Boolean;->booleanValue()Z

    .line 33554761
    .line 33554762
    .line 33554763
    move-result v3

    .line 33554764
    const-string v4, "RepostInputFragment"

    .line 33554765
    .line 33554766
    if-nez v3, :cond_382

    .line 33554767
    .line 33554768
    const-string v3, "close icon AB(repost_input_close_icon_enabled) off, skip render"

    .line 33554769
    .line 33554770
    invoke-static {v4, v3}, LX/0H6n;->LIZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 33554771
    .line 33554772
    .line 33554773
    :cond_155
    :goto_155
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJL:Lcom/bytedance/tux/widget/RadiusLayout;

    .line 33554774
    .line 33554775
    if-eqz v3, :cond_169

    .line 33554776
    .line 33554777
    invoke-virtual {v3}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 33554778
    .line 33554779
    .line 33554780
    move-result-object v4

    .line 33554781
    if-eqz v4, :cond_169

    .line 33554782
    .line 33554783
    invoke-static {}, LX/0kpR;->LIZ()Lcom/ss/android/ugc/aweme/comment/services/IKeyboardService;

    .line 33554784
    .line 33554785
    .line 33554786
    move-result-object v3

    .line 33554787
    invoke-interface {v3, v12}, Lcom/ss/android/ugc/aweme/comment/services/IKeyboardService;->LIZJ(Z)I

    .line 33554788
    .line 33554789
    .line 33554790
    move-result v3

    .line 33554791
    iput v3, v4, Landroid/view/ViewGroup$LayoutParams;->height:I

    .line 33554792
    .line 33554793
    :cond_169
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJL:Lcom/bytedance/tux/widget/RadiusLayout;

    .line 33554794
    .line 33554795
    if-eqz v3, :cond_170

    .line 33554796
    .line 33554797
    invoke-virtual {v3}, Landroid/view/View;->requestLayout()V

    .line 33554798
    .line 33554799
    .line 33554800
    :cond_170
    new-instance v4, Lkotlin/jvm/internal/AwS739S0100000_22;

    .line 33554801
    .line 33554802
    const/16 v3, 0x111

    .line 33554803
    .line 33554804
    invoke-direct {v4, v0, v3}, Lkotlin/jvm/internal/AwS739S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;I)V

    .line 33554805
    .line 33554806
    .line 33554807
    const/4 v5, 0x3

    .line 33554808
    invoke-static {v0, v12, v11, v4, v5}, LX/1Aww;->LIZ(Landroidx/fragment/app/Fragment;ZLX/1ATE;Lkotlin/jvm/functions/Function1;I)V

    .line 33554809
    .line 33554810
    .line 33554811
    invoke-static {}, LX/0pPM;->LIZIZ()Z

    .line 33554812
    .line 33554813
    .line 33554814
    move-result v3

    .line 33554815
    if-eqz v3, :cond_18b

    .line 33554816
    .line 33554817
    new-instance v4, Lkotlin/jvm/internal/AwS739S0100000_22;

    .line 33554818
    .line 33554819
    const/16 v3, 0x114

    .line 33554820
    .line 33554821
    invoke-direct {v4, v0, v3}, Lkotlin/jvm/internal/AwS739S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;I)V

    .line 33554822
    .line 33554823
    .line 33554824
    invoke-static {v0, v12, v11, v4, v5}, LX/1Aww;->LIZ(Landroidx/fragment/app/Fragment;ZLX/1ATE;Lkotlin/jvm/functions/Function1;I)V

    .line 33554825
    .line 33554826
    .line 33554827
    :cond_18b
    sget-object v3, LX/0opy;->LIZ:Ljava/util/HashMap;

    .line 33554828
    .line 33554829
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->lX()Ljava/lang/String;

    .line 33554830
    .line 33554831
    .line 33554832
    move-result-object v4

    .line 33554833
    if-nez v4, :cond_341

    .line 33554834
    .line 33554835
    move-object v3, v11

    .line 33554836
    :cond_194
    :goto_194
    iget-boolean v4, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLF:Z

    .line 33554837
    .line 33554838
    if-eqz v4, :cond_318

    .line 33554839
    .line 33554840
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJLIIIJLLLLLLLZ:LX/0HLX;

    .line 33554841
    .line 33554842
    if-eqz v5, :cond_1a6

    .line 33554843
    .line 33554844
    const v4, 0x7f117c3f

    .line 33554845
    .line 33554846
    .line 33554847
    invoke-virtual {v0, v4}, Landroidx/fragment/app/Fragment;->getText(I)Ljava/lang/CharSequence;

    .line 33554848
    .line 33554849
    .line 33554850
    move-result-object v4

    .line 33554851
    invoke-virtual {v5, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 33554852
    .line 33554853
    .line 33554854
    :cond_1a6
    :goto_1a6
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->jX()LX/0pSs;

    .line 33554855
    .line 33554856
    .line 33554857
    move-result-object v4

    .line 33554858
    invoke-virtual {v0, v3, v4}, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->CY1(Landroid/text/Editable;LX/0pSs;)V

    .line 33554859
    .line 33554860
    .line 33554861
    iget-object v8, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33554862
    .line 33554863
    if-eqz v8, :cond_1d6

    .line 33554864
    .line 33554865
    new-array v7, v2, [Landroid/text/InputFilter;

    .line 33554866
    .line 33554867
    invoke-static {}, LX/0IMG;->LIZJ()Z

    .line 33554868
    .line 33554869
    .line 33554870
    move-result v3

    .line 33554871
    if-eqz v3, :cond_2fe

    .line 33554872
    .line 33554873
    new-instance v6, LX/0HvS;

    .line 33554874
    .line 33554875
    sget-object v3, LX/08uW;->LIZ:LX/01SE;

    .line 33554876
    .line 33554877
    invoke-interface {v3}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33554878
    .line 33554879
    .line 33554880
    move-result-object v3

    .line 33554881
    check-cast v3, Ljava/lang/Number;

    .line 33554882
    .line 33554883
    invoke-virtual {v3}, Ljava/lang/Number;->intValue()I

    .line 33554884
    .line 33554885
    .line 33554886
    move-result v5

    .line 33554887
    new-instance v4, Lkotlin/jvm/internal/AwS700S0100000_22;

    .line 33554888
    .line 33554889
    const/16 v3, 0x237

    .line 33554890
    .line 33554891
    invoke-direct {v4, v0, v3}, Lkotlin/jvm/internal/AwS700S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;I)V

    .line 33554892
    .line 33554893
    .line 33554894
    invoke-direct {v6, v5, v4}, LX/0HvS;-><init>(ILkotlin/jvm/functions/Function0;)V

    .line 33554895
    .line 33554896
    .line 33554897
    :goto_1d1
    aput-object v6, v7, v12

    .line 33554898
    .line 33554899
    invoke-virtual {v8, v7}, Landroid/widget/TextView;->setFilters([Landroid/text/InputFilter;)V

    .line 33554900
    .line 33554901
    .line 33554902
    :cond_1d6
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33554903
    .line 33554904
    if-eqz v5, :cond_1e3

    .line 33554905
    .line 33554906
    new-instance v4, LX/0q67;

    .line 33554907
    .line 33554908
    const/4 v3, 0x6

    .line 33554909
    invoke-direct {v4, v0, v3}, LX/0q67;-><init>(Ljava/lang/Object;I)V

    .line 33554910
    .line 33554911
    .line 33554912
    invoke-virtual {v5, v4}, Landroid/widget/TextView;->addTextChangedListener(Landroid/text/TextWatcher;)V

    .line 33554913
    .line 33554914
    .line 33554915
    :cond_1e3
    sget-object v3, LX/08uR;->LIZ:LX/01SE;

    .line 33554916
    .line 33554917
    invoke-interface {v3}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33554918
    .line 33554919
    .line 33554920
    move-result-object v3

    .line 33554921
    check-cast v3, Ljava/lang/Boolean;

    .line 33554922
    .line 33554923
    invoke-virtual {v3}, Ljava/lang/Boolean;->booleanValue()Z

    .line 33554924
    .line 33554925
    .line 33554926
    move-result v3

    .line 33554927
    if-eqz v3, :cond_204

    .line 33554928
    .line 33554929
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33554930
    .line 33554931
    if-eqz v5, :cond_204

    .line 33554932
    .line 33554933
    new-instance v4, LX/0IN5;

    .line 33554934
    .line 33554935
    const/4 v3, 0x5

    .line 33554936
    invoke-direct {v4, v0, v5, v3}, LX/0IN5;-><init>(Ljava/lang/Object;Ljava/lang/Object;I)V

    .line 33554937
    .line 33554938
    .line 33554939
    invoke-virtual {v5}, Landroid/view/View;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    .line 33554940
    .line 33554941
    .line 33554942
    move-result-object v3

    .line 33554943
    invoke-static {v3, v4}, LX/0WMW;->z3(Landroid/view/ViewTreeObserver;Landroid/view/ViewTreeObserver$OnPreDrawListener;)V

    .line 33554944
    .line 33554945
    .line 33554946
    iput-object v4, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLL:LX/0IN5;

    .line 33554947
    .line 33554948
    :cond_204
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33554949
    .line 33554950
    if-eqz v3, :cond_20b

    .line 33554951
    .line 33554952
    invoke-static {v3, v0}, LX/0WMW;->i9(LX/0I0C;Landroid/view/View$OnClickListener;)V

    .line 33554953
    .line 33554954
    .line 33554955
    :cond_20b
    invoke-static {v0, v11}, LX/1Azs;->LJFF(Landroidx/fragment/app/Fragment;Ljava/lang/String;)LX/0R3g;

    .line 33554956
    .line 33554957
    .line 33554958
    move-result-object v4

    .line 33554959
    const-class v3, Lcom/ss/android/ugc/aweme/upvote/detail/input/IRepostStickerAbility;

    .line 33554960
    .line 33554961
    invoke-static {v4, v3, v11}, LX/1Axa;->LIZ(LX/0R3g;Ljava/lang/Class;Ljava/lang/String;)LX/01xw;

    .line 33554962
    .line 33554963
    .line 33554964
    move-result-object v3

    .line 33554965
    check-cast v3, Lcom/ss/android/ugc/aweme/upvote/detail/input/IRepostStickerAbility;

    .line 33554966
    .line 33554967
    if-eqz v3, :cond_21c

    .line 33554968
    .line 33554969
    invoke-interface {v3}, Lcom/ss/android/ugc/aweme/upvote/detail/input/IRepostStickerAbility;->c71()V

    .line 33554970
    .line 33554971
    .line 33554972
    :cond_21c
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJLIIL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554973
    .line 33554974
    if-eqz v3, :cond_223

    .line 33554975
    .line 33554976
    invoke-static {v3, v0}, LX/0WMW;->f9(Lcom/bytedance/tux/icon/TuxIconView;Landroid/view/View$OnClickListener;)V

    .line 33554977
    .line 33554978
    .line 33554979
    :cond_223
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJJLIIL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554980
    .line 33554981
    if-eqz v3, :cond_22a

    .line 33554982
    .line 33554983
    invoke-static {v3}, LX/0Cqc;->LIZLLL(Landroid/view/View;)V

    .line 33554984
    .line 33554985
    .line 33554986
    :cond_22a
    iget-object v7, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33554987
    .line 33554988
    if-eqz v7, :cond_251

    .line 33554989
    .line 33554990
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554991
    .line 33554992
    .line 33554993
    move-result-object v8

    .line 33554994
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554995
    .line 33554996
    .line 33554997
    move-result-object v9

    .line 33554998
    const/16 v3, 0xc

    .line 33554999
    .line 33555000
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555001
    .line 33555002
    .line 33555003
    move-result-object v3

    .line 33555004
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33555005
    .line 33555006
    .line 33555007
    move-result v3

    .line 33555008
    invoke-static {v3}, LX/0MTS;->LIZJ(F)I

    .line 33555009
    .line 33555010
    .line 33555011
    move-result v3

    .line 33555012
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555013
    .line 33555014
    .line 33555015
    move-result-object v10

    .line 33555016
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555017
    .line 33555018
    .line 33555019
    move-result-object v11

    .line 33555020
    const/16 v13, 0x10

    .line 33555021
    .line 33555022
    invoke-static/range {v7 .. v13}, LX/0GtA;->LJIIIZ(Landroid/view/View;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;ZI)V

    .line 33555023
    .line 33555024
    .line 33555025
    :cond_251
    sget-object v3, LX/0pPE;->LIZ:LX/0pPE;

    .line 33555026
    .line 33555027
    iget-object v4, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJLLL:Ljava/lang/String;

    .line 33555028
    .line 33555029
    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 33555030
    .line 33555031
    .line 33555032
    const-string v3, "show"

    .line 33555033
    .line 33555034
    invoke-static {v3, v4}, LX/0pPE;->LJFF(Ljava/lang/String;Ljava/lang/String;)V

    .line 33555035
    .line 33555036
    .line 33555037
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJL:Lcom/bytedance/tux/icon/TuxIconView;

    .line 33555038
    .line 33555039
    if-eqz v3, :cond_264

    .line 33555040
    .line 33555041
    invoke-static {v3, v0}, LX/0WMW;->f9(Lcom/bytedance/tux/icon/TuxIconView;Landroid/view/View$OnClickListener;)V

    .line 33555042
    .line 33555043
    .line 33555044
    :cond_264
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJLIIIJLLLLLLLZ:LX/0HLX;

    .line 33555045
    .line 33555046
    if-eqz v3, :cond_26b

    .line 33555047
    .line 33555048
    invoke-static {v3, v0}, LX/0WMW;->Z8(LX/0HLX;Landroid/view/View$OnClickListener;)V

    .line 33555049
    .line 33555050
    .line 33555051
    :cond_26b
    invoke-static {v0, v1}, LX/0WMW;->O7(Landroid/view/View$OnClickListener;Landroid/view/View;)V

    .line 33555052
    .line 33555053
    .line 33555054
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555055
    .line 33555056
    if-eqz v3, :cond_275

    .line 33555057
    .line 33555058
    invoke-virtual {v3}, Landroid/view/View;->requestFocus()Z

    .line 33555059
    .line 33555060
    .line 33555061
    :cond_275
    iget-object v3, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIII:LX/0oqF;

    .line 33555062
    .line 33555063
    sget-object v4, LX/0oqE;->LIZ:[I

    .line 33555064
    .line 33555065
    invoke-virtual {v3}, Ljava/lang/Enum;->ordinal()I

    .line 33555066
    .line 33555067
    .line 33555068
    move-result v3

    .line 33555069
    aget v6, v4, v3

    .line 33555070
    .line 33555071
    const/4 v5, 0x5

    .line 33555072
    const-wide/16 v3, 0x0

    .line 33555073
    .line 33555074
    if-eq v6, v5, :cond_2ea

    .line 33555075
    .line 33555076
    const/4 v5, 0x7

    .line 33555077
    if-ne v6, v5, :cond_2a4

    .line 33555078
    .line 33555079
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->pX()V

    .line 33555080
    .line 33555081
    .line 33555082
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555083
    .line 33555084
    if-eqz v5, :cond_291

    .line 33555085
    .line 33555086
    invoke-virtual {v5, v12}, Landroid/widget/TextView;->setShowSoftInputOnFocus(Z)V

    .line 33555087
    .line 33555088
    .line 33555089
    :cond_291
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555090
    .line 33555091
    if-eqz v5, :cond_298

    .line 33555092
    .line 33555093
    invoke-virtual {v5, v2}, Landroid/view/View;->setFocusable(Z)V

    .line 33555094
    .line 33555095
    .line 33555096
    :cond_298
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555097
    .line 33555098
    if-eqz v5, :cond_29f

    .line 33555099
    .line 33555100
    invoke-virtual {v5, v2}, Landroid/view/View;->setFocusableInTouchMode(Z)V

    .line 33555101
    .line 33555102
    .line 33555103
    :cond_29f
    iget-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555104
    .line 33555105
    invoke-static {v2}, Lcom/ss/android/ugc/aweme/base/utils/KeyboardUtils;->LIZIZ(Landroid/view/View;)V

    .line 33555106
    .line 33555107
    .line 33555108
    :cond_2a4
    :goto_2a4
    invoke-static {}, LX/0pEb;->LIZ()Z

    .line 33555109
    .line 33555110
    .line 33555111
    move-result v2

    .line 33555112
    if-nez v2, :cond_2c6

    .line 33555113
    .line 33555114
    invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;

    .line 33555115
    .line 33555116
    .line 33555117
    move-result-object v5

    .line 33555118
    const v2, 0x7f0201ac

    .line 33555119
    .line 33555120
    .line 33555121
    invoke-static {v5, v2}, Landroid/view/animation/AnimationUtils;->loadAnimation(Landroid/content/Context;I)Landroid/view/animation/Animation;

    .line 33555122
    .line 33555123
    .line 33555124
    move-result-object v5

    .line 33555125
    new-instance v2, LX/0oq7;

    .line 33555126
    .line 33555127
    invoke-direct {v2, v0}, LX/0oq7;-><init>(Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;)V

    .line 33555128
    .line 33555129
    .line 33555130
    invoke-virtual {v5, v2}, Landroid/view/animation/Animation;->setAnimationListener(Landroid/view/animation/Animation$AnimationListener;)V

    .line 33555131
    .line 33555132
    .line 33555133
    invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->getView()Landroid/view/View;

    .line 33555134
    .line 33555135
    .line 33555136
    move-result-object v0

    .line 33555137
    if-eqz v0, :cond_2c6

    .line 33555138
    .line 33555139
    invoke-virtual {v0, v5}, Landroid/view/View;->startAnimation(Landroid/view/animation/Animation;)V

    .line 33555140
    .line 33555141
    .line 33555142
    :cond_2c6
    sget-wide v5, LX/0opx;->LIZ:J

    .line 33555143
    .line 33555144
    cmp-long v0, v5, v3

    .line 33555145
    .line 33555146
    if-eqz v0, :cond_2d2

    .line 33555147
    .line 33555148
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 33555149
    .line 33555150
    .line 33555151
    move-result-wide v2

    .line 33555152
    sput-wide v2, LX/0opx;->LIZLLL:J

    .line 33555153
    .line 33555154
    :cond_2d2
    invoke-virtual {v1}, Landroid/view/View;->getViewTreeObserver()Landroid/view/ViewTreeObserver;

    .line 33555155
    .line 33555156
    .line 33555157
    move-result-object v3

    .line 33555158
    invoke-virtual {v3}, Landroid/view/ViewTreeObserver;->isAlive()Z

    .line 33555159
    .line 33555160
    .line 33555161
    move-result v0

    .line 33555162
    if-eqz v0, :cond_2e9

    .line 33555163
    .line 33555164
    new-instance v2, LX/0HJm;

    .line 33555165
    .line 33555166
    invoke-direct {v2, v1}, LX/0HJm;-><init>(Landroid/view/View;)V

    .line 33555167
    .line 33555168
    .line 33555169
    invoke-virtual {v3, v2}, Landroid/view/ViewTreeObserver;->addOnDrawListener(Landroid/view/ViewTreeObserver$OnDrawListener;)V

    .line 33555170
    .line 33555171
    .line 33555172
    const-class v0, Landroid/view/ViewTreeObserver$OnDrawListener;

    .line 33555173
    .line 33555174
    invoke-static {v3, v2, v0}, LX/1H21;->LIZ(Landroid/view/ViewTreeObserver;Ljava/lang/Object;Ljava/lang/Class;)V

    .line 33555175
    .line 33555176
    .line 33555177
    :cond_2e9
    return-void

    .line 33555178
    :cond_2ea
    iget-object v6, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555179
    .line 33555180
    if-eqz v6, :cond_2f8

    .line 33555181
    .line 33555182
    new-instance v5, LY/ARunnableS156S0100000_6;

    .line 33555183
    .line 33555184
    const/16 v2, 0x8e

    .line 33555185
    .line 33555186
    invoke-direct {v5, v6, v2}, LY/ARunnableS156S0100000_6;-><init>(Ljava/lang/Object;I)V

    .line 33555187
    .line 33555188
    .line 33555189
    invoke-static {v6, v5, v3, v4}, Lcom/bytedance/bpea/transmit/hook/HandlerHook;->viewPostDelay(Landroid/view/View;Ljava/lang/Runnable;J)Z

    .line 33555190
    .line 33555191
    .line 33555192
    :cond_2f8
    iget-object v2, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555193
    .line 33555194
    invoke-static {v2}, Lcom/ss/android/ugc/aweme/base/utils/KeyboardUtils;->LIZLLL(Landroid/view/View;)V

    .line 33555195
    .line 33555196
    .line 33555197
    goto :goto_2a4

    .line 33555198
    :cond_2fe
    new-instance v6, LX/0H87;

    .line 33555199
    .line 33555200
    sget-object v3, LX/08uW;->LIZ:LX/01SE;

    .line 33555201
    .line 33555202
    invoke-interface {v3}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33555203
    .line 33555204
    .line 33555205
    move-result-object v3

    .line 33555206
    check-cast v3, Ljava/lang/Number;

    .line 33555207
    .line 33555208
    invoke-virtual {v3}, Ljava/lang/Number;->intValue()I

    .line 33555209
    .line 33555210
    .line 33555211
    move-result v5

    .line 33555212
    new-instance v4, Lkotlin/jvm/internal/AwS700S0100000_22;

    .line 33555213
    .line 33555214
    const/16 v3, 0x238

    .line 33555215
    .line 33555216
    invoke-direct {v4, v0, v3}, Lkotlin/jvm/internal/AwS700S0100000_22;-><init>(Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;I)V

    .line 33555217
    .line 33555218
    .line 33555219
    invoke-direct {v6, v5, v4}, LX/0H87;-><init>(ILkotlin/jvm/functions/Function0;)V

    .line 33555220
    .line 33555221
    .line 33555222
    goto/16 :goto_1d1

    .line 33555223
    .line 33555224
    :cond_318
    iget-object v5, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJLIIIJLLLLLLLZ:LX/0HLX;

    .line 33555225
    .line 33555226
    if-eqz v5, :cond_326

    .line 33555227
    .line 33555228
    const v4, 0x7f116cf9

    .line 33555229
    .line 33555230
    .line 33555231
    invoke-virtual {v0, v4}, Landroidx/fragment/app/Fragment;->getText(I)Ljava/lang/CharSequence;

    .line 33555232
    .line 33555233
    .line 33555234
    move-result-object v4

    .line 33555235
    invoke-virtual {v5, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 33555236
    .line 33555237
    .line 33555238
    :cond_326
    sget-object v6, LX/0pP4;->LIZ:LX/0pP4;

    .line 33555239
    .line 33555240
    iget-object v7, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJLLL:Ljava/lang/String;

    .line 33555241
    .line 33555242
    const-string v8, "click_recommend_avatar"

    .line 33555243
    .line 33555244
    iget-object v9, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJLL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 33555245
    .line 33555246
    const-string v10, "recommend_panel"

    .line 33555247
    .line 33555248
    const-wide/16 v13, 0x0

    .line 33555249
    .line 33555250
    const-string v15, "card"

    .line 33555251
    .line 33555252
    const-string v16, "text_input_panel"

    .line 33555253
    .line 33555254
    const/16 v19, 0x670

    .line 33555255
    .line 33555256
    move-object/from16 v17, v11

    .line 33555257
    .line 33555258
    move-object/from16 v18, v11

    .line 33555259
    .line 33555260
    invoke-static/range {v6 .. v19}, LX/0pP4;->LIZIZ(LX/0pP4;Ljava/lang/String;Ljava/lang/String;Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;Ljava/lang/String;ZDLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V

    .line 33555261
    .line 33555262
    .line 33555263
    goto/16 :goto_1a6

    .line 33555264
    .line 33555265
    :cond_341
    sget-object v3, LX/0opy;->LIZ:Ljava/util/HashMap;

    .line 33555266
    .line 33555267
    invoke-virtual {v3, v4}, Ljava/util/HashMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    .line 33555268
    .line 33555269
    .line 33555270
    move-result-object v3

    .line 33555271
    check-cast v3, Landroid/text/Editable;

    .line 33555272
    .line 33555273
    if-eqz v3, :cond_194

    .line 33555274
    .line 33555275
    invoke-interface {v3}, Ljava/lang/CharSequence;->length()I

    .line 33555276
    .line 33555277
    .line 33555278
    move-result v4

    .line 33555279
    if-eqz v4, :cond_194

    .line 33555280
    .line 33555281
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->kX()Lcom/ss/android/ugc/aweme/upvote/detail/input/IRepostMentionAbility;

    .line 33555282
    .line 33555283
    .line 33555284
    move-result-object v4

    .line 33555285
    if-eqz v4, :cond_35a

    .line 33555286
    .line 33555287
    invoke-interface {v4, v3}, Lcom/ss/android/ugc/aweme/upvote/detail/input/IRepostMentionAbility;->f61(Landroid/text/Editable;)V

    .line 33555288
    .line 33555289
    .line 33555290
    :cond_35a
    sget-object v4, LX/08uR;->LIZ:LX/01SE;

    .line 33555291
    .line 33555292
    invoke-interface {v4}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33555293
    .line 33555294
    .line 33555295
    move-result-object v4

    .line 33555296
    check-cast v4, Ljava/lang/Boolean;

    .line 33555297
    .line 33555298
    invoke-virtual {v4}, Ljava/lang/Boolean;->booleanValue()Z

    .line 33555299
    .line 33555300
    .line 33555301
    move-result v4

    .line 33555302
    if-nez v4, :cond_194

    .line 33555303
    .line 33555304
    iget-object v4, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555305
    .line 33555306
    if-eqz v4, :cond_194

    .line 33555307
    .line 33555308
    invoke-virtual {v4}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    .line 33555309
    .line 33555310
    .line 33555311
    move-result-object v4

    .line 33555312
    if-eqz v4, :cond_194

    .line 33555313
    .line 33555314
    iget-object v6, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLIIIIL:LX/0oni;

    .line 33555315
    .line 33555316
    if-eqz v6, :cond_194

    .line 33555317
    .line 33555318
    new-instance v5, LY/ARunnableS172S0100000_22;

    .line 33555319
    .line 33555320
    const/16 v4, 0x61

    .line 33555321
    .line 33555322
    invoke-direct {v5, v0, v4}, LY/ARunnableS172S0100000_22;-><init>(Ljava/lang/Object;I)V

    .line 33555323
    .line 33555324
    .line 33555325
    invoke-static {v6, v5}, Lcom/bytedance/bpea/transmit/hook/HandlerHook;->viewPost(Landroid/view/View;Ljava/lang/Runnable;)Z

    .line 33555326
    .line 33555327
    .line 33555328
    goto/16 :goto_194

    .line 33555329
    .line 33555330
    :cond_382
    iget-object v7, v0, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLJJJIL:Landroid/widget/LinearLayout;

    .line 33555331
    .line 33555332
    if-eqz v7, :cond_155

    .line 33555333
    .line 33555334
    invoke-virtual {v7}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    .line 33555335
    .line 33555336
    .line 33555337
    move-result-object v5

    .line 33555338
    instance-of v3, v5, Landroid/widget/LinearLayout;

    .line 33555339
    .line 33555340
    if-eqz v3, :cond_155

    .line 33555341
    .line 33555342
    check-cast v5, Landroid/view/ViewGroup;

    .line 33555343
    .line 33555344
    if-eqz v5, :cond_155

    .line 33555345
    .line 33555346
    invoke-virtual {v1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 33555347
    .line 33555348
    .line 33555349
    move-result-object v14

    .line 33555350
    const v3, 0x7f060399

    .line 33555351
    .line 33555352
    .line 33555353
    invoke-static {v3, v14}, LX/0GzZ;->LIZJ(ILandroid/content/Context;)Ljava/lang/Integer;

    .line 33555354
    .line 33555355
    .line 33555356
    move-result-object v3

    .line 33555357
    if-eqz v3, :cond_440

    .line 33555358
    .line 33555359
    invoke-virtual {v3}, Ljava/lang/Integer;->intValue()I

    .line 33555360
    .line 33555361
    .line 33555362
    move-result v8

    .line 33555363
    :goto_3a3
    new-instance v13, Lcom/bytedance/tux/icon/TuxIconView;

    .line 33555364
    .line 33555365
    const/16 v17, 0x6

    .line 33555366
    .line 33555367
    move-object v15, v11

    .line 33555368
    move/from16 v16, v12

    .line 33555369
    .line 33555370
    move-object/from16 v18, v11

    .line 33555371
    .line 33555372
    invoke-direct/range {v13 .. v18}, Lcom/bytedance/tux/icon/TuxIconView;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;IILkotlin/jvm/internal/DefaultConstructorMarker;)V

    .line 33555373
    .line 33555374
    .line 33555375
    invoke-static/range {v17 .. v17}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555376
    .line 33555377
    .line 33555378
    move-result-object v3

    .line 33555379
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33555380
    .line 33555381
    .line 33555382
    move-result v3

    .line 33555383
    invoke-static {v3}, LX/0MTS;->LIZJ(F)I

    .line 33555384
    .line 33555385
    .line 33555386
    move-result v3

    .line 33555387
    invoke-virtual {v13, v3, v3, v3, v3}, Landroid/view/View;->setPadding(IIII)V

    .line 33555388
    .line 33555389
    .line 33555390
    sget-object v3, Lcom/ss/android/ugc/aweme/upvote/detail/input/RepostInputFragment;->LLLLIILL:LX/0GtW;

    .line 33555391
    .line 33555392
    invoke-virtual {v13, v3}, Lcom/bytedance/tux/icon/TuxIconView;->setTuxIcon(LX/0GtW;)V

    .line 33555393
    .line 33555394
    .line 33555395
    new-instance v6, Landroid/graphics/drawable/GradientDrawable;

    .line 33555396
    .line 33555397
    invoke-direct {v6}, Landroid/graphics/drawable/GradientDrawable;-><init>()V

    .line 33555398
    .line 33555399
    .line 33555400
    const/16 v3, 0x12

    .line 33555401
    .line 33555402
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555403
    .line 33555404
    .line 33555405
    move-result-object v3

    .line 33555406
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33555407
    .line 33555408
    .line 33555409
    move-result v3

    .line 33555410
    invoke-virtual {v6, v3}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V

    .line 33555411
    .line 33555412
    .line 33555413
    invoke-virtual {v6, v8}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V

    .line 33555414
    .line 33555415
    .line 33555416
    invoke-virtual {v13, v6}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V

    .line 33555417
    .line 33555418
    .line 33555419
    invoke-virtual {v13, v2}, Landroid/view/View;->setClickable(Z)V

    .line 33555420
    .line 33555421
    .line 33555422
    invoke-virtual {v13, v2}, Landroid/view/View;->setFocusable(Z)V

    .line 33555423
    .line 33555424
    .line 33555425
    new-instance v6, LY/ACListenerS208S0100000_22;

    .line 33555426
    .line 33555427
    const/16 v3, 0x6d

    .line 33555428
    .line 33555429
    invoke-direct {v6, v0, v3}, LY/ACListenerS208S0100000_22;-><init>(Ljava/lang/Object;I)V

    .line 33555430
    .line 33555431
    .line 33555432
    invoke-static {v13, v6}, LX/0WMW;->f9(Lcom/bytedance/tux/icon/TuxIconView;Landroid/view/View$OnClickListener;)V

    .line 33555433
    .line 33555434
    .line 33555435
    invoke-virtual {v7}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    .line 33555436
    .line 33555437
    .line 33555438
    move-result-object v6

    .line 33555439
    instance-of v3, v6, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 33555440
    .line 33555441
    if-eqz v3, :cond_43e

    .line 33555442
    .line 33555443
    check-cast v6, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 33555444
    .line 33555445
    if-eqz v6, :cond_43e

    .line 33555446
    .line 33555447
    iget v8, v6, Landroid/view/ViewGroup$MarginLayoutParams;->bottomMargin:I

    .line 33555448
    .line 33555449
    :goto_3f9
    new-instance v6, Landroid/widget/LinearLayout$LayoutParams;

    .line 33555450
    .line 33555451
    const/16 v10, 0x24

    .line 33555452
    .line 33555453
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555454
    .line 33555455
    .line 33555456
    move-result-object v3

    .line 33555457
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33555458
    .line 33555459
    .line 33555460
    move-result v3

    .line 33555461
    invoke-static {v3}, LX/0MTS;->LIZJ(F)I

    .line 33555462
    .line 33555463
    .line 33555464
    move-result v9

    .line 33555465
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555466
    .line 33555467
    .line 33555468
    move-result-object v3

    .line 33555469
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33555470
    .line 33555471
    .line 33555472
    move-result v3

    .line 33555473
    invoke-static {v3}, LX/0MTS;->LIZJ(F)I

    .line 33555474
    .line 33555475
    .line 33555476
    move-result v3

    .line 33555477
    invoke-direct {v6, v9, v3}, Landroid/widget/LinearLayout$LayoutParams;-><init>(II)V

    .line 33555478
    .line 33555479
    .line 33555480
    iput v2, v6, Landroid/widget/LinearLayout$LayoutParams;->gravity:I

    .line 33555481
    .line 33555482
    const/16 v3, 0x10

    .line 33555483
    .line 33555484
    invoke-static {v3}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33555485
    .line 33555486
    .line 33555487
    move-result-object v3

    .line 33555488
    invoke-static {v3}, LX/0DZx;->LIZIZ(Ljava/lang/Number;)F

    .line 33555489
    .line 33555490
    .line 33555491
    move-result v3

    .line 33555492
    invoke-static {v3}, LX/0MTS;->LIZJ(F)I

    .line 33555493
    .line 33555494
    .line 33555495
    move-result v3

    .line 33555496
    sub-int/2addr v3, v8

    .line 33555497
    if-gez v3, :cond_42c

    .line 33555498
    .line 33555499
    const/4 v3, 0x0

    .line 33555500
    :cond_42c
    iput v3, v6, Landroid/widget/LinearLayout$LayoutParams;->topMargin:I

    .line 33555501
    .line 33555502
    invoke-virtual {v5, v7}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I

    .line 33555503
    .line 33555504
    .line 33555505
    move-result v3

    .line 33555506
    add-int/lit8 v3, v3, 0x1

    .line 33555507
    .line 33555508
    invoke-virtual {v5, v13, v3, v6}, Landroid/view/ViewGroup;->addView(Landroid/view/View;ILandroid/view/ViewGroup$LayoutParams;)V

    .line 33555509
    .line 33555510
    .line 33555511
    const-string v3, "close icon rendered below input box (AB on)"

    .line 33555512
    .line 33555513
    invoke-static {v4, v3}, LX/0H6n;->LIZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 33555514
    .line 33555515
    .line 33555516
    goto/16 :goto_155

    .line 33555517
    .line 33555518
    :cond_43e
    const/4 v8, 0x0

    .line 33555519
    goto :goto_3f9

    .line 33555520
    :cond_440
    const/4 v8, 0x0

    .line 33555521
    goto/16 :goto_3a3

    .line 33555522
    .line 33555523
    :cond_443
    if-nez v6, :cond_46d

    .line 33555524
    .line 33555525
    sget-object v3, LX/08uf;->LIZ:LX/01SE;

    .line 33555526
    .line 33555527
    invoke-interface {v3}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33555528
    .line 33555529
    .line 33555530
    move-result-object v3

    .line 33555531
    check-cast v3, Ljava/lang/Number;

    .line 33555532
    .line 33555533
    invoke-virtual {v3}, Ljava/lang/Number;->intValue()I

    .line 33555534
    .line 33555535
    .line 33555536
    move-result v3

    .line 33555537
    if-nez v3, :cond_46d

    .line 33555538
    .line 33555539
    invoke-static {}, LX/0oqI;->LIZIZ()Lcom/bytedance/keva/Keva;

    .line 33555540
    .line 33555541
    .line 33555542
    move-result-object v4

    .line 33555543
    invoke-static {v7}, LX/0oqI;->LIZ(Ljava/lang/String;)Ljava/lang/String;

    .line 33555544
    .line 33555545
    .line 33555546
    move-result-object v3

    .line 33555547
    invoke-virtual {v4, v3, v2}, Lcom/bytedance/keva/Keva;->storeBoolean(Ljava/lang/String;Z)V

    .line 33555548
    .line 33555549
    .line 33555550
    invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;

    .line 33555551
    .line 33555552
    .line 33555553
    move-result-object v4

    .line 33555554
    if-eqz v4, :cond_499

    .line 33555555
    .line 33555556
    const v3, 0x7f117c40

    .line 33555557
    .line 33555558
    .line 33555559
    invoke-virtual {v4, v3}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    .line 33555560
    .line 33555561
    .line 33555562
    move-result-object v3

    .line 33555563
    goto/16 :goto_c7

    .line 33555564
    .line 33555565
    :cond_46d
    sget-object v3, LX/08Cq;->LIZ:LX/01SE;

    .line 33555566
    .line 33555567
    invoke-interface {v3}, LX/01SE;->getValue()Ljava/lang/Object;

    .line 33555568
    .line 33555569
    .line 33555570
    move-result-object v3

    .line 33555571
    check-cast v3, Ljava/lang/Boolean;

    .line 33555572
    .line 33555573
    invoke-virtual {v3}, Ljava/lang/Boolean;->booleanValue()Z

    .line 33555574
    .line 33555575
    .line 33555576
    move-result v3

    .line 33555577
    if-eqz v3, :cond_48a

    .line 33555578
    .line 33555579
    invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;

    .line 33555580
    .line 33555581
    .line 33555582
    move-result-object v4

    .line 33555583
    if-eqz v4, :cond_499

    .line 33555584
    .line 33555585
    const v3, 0x7f111304

    .line 33555586
    .line 33555587
    .line 33555588
    invoke-virtual {v4, v3}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    .line 33555589
    .line 33555590
    .line 33555591
    move-result-object v3

    .line 33555592
    goto/16 :goto_c7

    .line 33555593
    .line 33555594
    :cond_48a
    invoke-virtual {v0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;

    .line 33555595
    .line 33555596
    .line 33555597
    move-result-object v4

    .line 33555598
    if-eqz v4, :cond_499

    .line 33555599
    .line 33555600
    const v3, 0x7f117c41

    .line 33555601
    .line 33555602
    .line 33555603
    invoke-virtual {v4, v3}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    .line 33555604
    .line 33555605
    .line 33555606
    move-result-object v3

    .line 33555607
    goto/16 :goto_c7

    .line 33555608
    .line 33555609
    :cond_499
    const-string v3, ""

    .line 33555610
    .line 33555611
    goto/16 :goto_c9
.end method
