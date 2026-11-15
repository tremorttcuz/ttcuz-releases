.class public LX/08Vk;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements LX/08Vj;
.implements LX/08Vf;
.implements Lcom/ss/android/ugc/aweme/casting/api/ICastingSeekListener;


# static fields
.field public static LLLLJ:I


# instance fields
.field public final LL:LX/08Ic;

.field public final LLJJIJIIJIL:Landroid/view/ViewGroup;

.field public final LLJJIJIL:Landroid/view/View;

.field public final LLJJJ:Landroid/widget/ImageView;

.field public final LLJJJIL:Lcom/ss/android/ugc/aweme/homepage/api/interaction/ScrollSwitchStateManager;

.field public final LLJJJJ:Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;

.field public final LLJJJJJIL:LX/08WH;

.field public final LLJJJJLIIL:LX/089c;

.field public final LLJJL:LX/02pj;

.field public final LLJJLIIIJLLLLLLLZ:LX/02pj;

.field public final LLJL:LX/02pj;

.field public final LLJLIL:I

.field public final LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

.field public final LLJLL:LX/02pj;

.field public LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

.field public final LLJLLL:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List<",
            "Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;",
            ">;"
        }
    .end annotation
.end field

.field public LLJZ:I

.field public LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

.field public LLL:I

.field public LLLF:Z

.field public LLLFF:Z

.field public LLLFFI:Z

.field public LLLFZ:J

.field public LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

.field public LLLII:Ljava/lang/String;

.field public LLLIIII:Ljava/lang/String;

.field public LLLIIIIL:I

.field public LLLIIIL:I

.field public LLLIIL:Z

.field public LLLIILIL:Z

.field public LLLIL:Z

.field public LLLILZ:Z

.field public LLLILZJ:I

.field public LLLILZLLLI:Z

.field public LLLIZZ:I

.field public LLLJ:J

.field public LLLJIL:Z

.field public LLLJL:I

.field public LLLL:J

.field public LLLLII:Z

.field public LLLLIIIILLL:LX/0Lxl;

.field public LLLLIIL:Z

.field public LLLLIILL:LX/08WY;

.field public LLLLIILLL:Z

.field public LLLLIL:Z

.field public final LLLLILI:LX/08Xl;


# direct methods
.method public static constructor <clinit>()V
    .registers 0

    return-void
.end method

.method public constructor <init>(LX/08Ic;Landroid/view/ViewGroup;LX/07Az;Landroid/widget/ImageView;Lcom/ss/android/ugc/aweme/homepage/api/interaction/ScrollSwitchStateManager;Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;LX/08WH;)V
    .registers 27

    .prologue
    .line 117440512
    move-object/from16 v1, p0

    .line 117440513
    .line 117440514
    invoke-direct {v1}, Ljava/lang/Object;-><init>()V

    .line 117440515
    .line 117440516
    .line 117440517
    sget v9, LX/036O;->LIZ:I

    .line 117440518
    .line 117440519
    move-object/from16 v5, p1

    .line 117440520
    .line 117440521
    iput-object v5, v1, LX/08Vk;->LL:LX/08Ic;

    .line 117440522
    .line 117440523
    move-object/from16 v4, p2

    .line 117440524
    .line 117440525
    iput-object v4, v1, LX/08Vk;->LLJJIJIIJIL:Landroid/view/ViewGroup;

    .line 117440526
    .line 117440527
    move-object/from16 v6, p3

    .line 117440528
    .line 117440529
    iput-object v6, v1, LX/08Vk;->LLJJIJIL:Landroid/view/View;

    .line 117440530
    .line 117440531
    move-object/from16 v12, p4

    .line 117440532
    .line 117440533
    iput-object v12, v1, LX/08Vk;->LLJJJ:Landroid/widget/ImageView;

    .line 117440534
    .line 117440535
    move-object/from16 v0, p5

    .line 117440536
    .line 117440537
    iput-object v0, v1, LX/08Vk;->LLJJJIL:Lcom/ss/android/ugc/aweme/homepage/api/interaction/ScrollSwitchStateManager;

    .line 117440538
    .line 117440539
    move-object/from16 v0, p6

    .line 117440540
    .line 117440541
    iput-object v0, v1, LX/08Vk;->LLJJJJ:Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;

    .line 117440542
    .line 117440543
    move-object/from16 v2, p7

    .line 117440544
    .line 117440545
    iput-object v2, v1, LX/08Vk;->LLJJJJJIL:LX/08WH;

    .line 117440546
    .line 117440547
    new-instance v7, LX/089c;

    .line 117440548
    .line 117440549
    invoke-virtual {v1}, LX/08Vk;->LJIIL()Ljava/lang/String;

    .line 117440550
    .line 117440551
    .line 117440552
    move-result-object v0

    .line 117440553
    invoke-direct {v7, v0}, LX/089c;-><init>(Ljava/lang/String;)V

    .line 117440554
    .line 117440555
    .line 117440556
    iput-object v7, v1, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 117440557
    .line 117440558
    const/4 v8, 0x0

    .line 117440559
    if-gez v9, :cond_1ce

    .line 117440560
    .line 117440561
    new-instance v0, LX/08Vr;

    .line 117440562
    .line 117440563
    invoke-direct {v0, v1}, LX/08Vr;-><init>(LX/08Vk;)V

    .line 117440564
    .line 117440565
    .line 117440566
    invoke-static {v0}, LX/02ph;->LIZIZ(Lkotlin/jvm/functions/Function0;)LX/02pj;

    .line 117440567
    .line 117440568
    .line 117440569
    move-result-object v0

    .line 117440570
    :goto_3a
    iput-object v0, v1, LX/08Vk;->LLJJL:LX/02pj;

    .line 117440571
    .line 117440572
    if-gez v9, :cond_1cb

    .line 117440573
    .line 117440574
    new-instance v0, LX/08Vt;

    .line 117440575
    .line 117440576
    invoke-direct {v0}, LX/08Vt;-><init>()V

    .line 117440577
    .line 117440578
    .line 117440579
    invoke-static {v0}, LX/02ph;->LIZIZ(Lkotlin/jvm/functions/Function0;)LX/02pj;

    .line 117440580
    .line 117440581
    .line 117440582
    move-result-object v0

    .line 117440583
    :goto_47
    iput-object v0, v1, LX/08Vk;->LLJJLIIIJLLLLLLLZ:LX/02pj;

    .line 117440584
    .line 117440585
    if-gez v9, :cond_1c8

    .line 117440586
    .line 117440587
    new-instance v0, LX/08Vx;

    .line 117440588
    .line 117440589
    invoke-direct {v0}, LX/08Vx;-><init>()V

    .line 117440590
    .line 117440591
    .line 117440592
    invoke-static {v0}, LX/02ph;->LIZIZ(Lkotlin/jvm/functions/Function0;)LX/02pj;

    .line 117440593
    .line 117440594
    .line 117440595
    move-result-object v0

    .line 117440596
    :goto_54
    iput-object v0, v1, LX/08Vk;->LLJL:LX/02pj;

    .line 117440597
    .line 117440598
    invoke-virtual {v5}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 117440599
    .line 117440600
    .line 117440601
    move-result-object v0

    .line 117440602
    invoke-static {v0}, LX/06Tr;->LJIIJ(Landroid/content/Context;)I

    .line 117440603
    .line 117440604
    .line 117440605
    move-result v0

    .line 117440606
    iput v0, v1, LX/08Vk;->LLJLIL:I

    .line 117440607
    .line 117440608
    new-instance v3, Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 117440609
    .line 117440610
    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    .line 117440611
    .line 117440612
    .line 117440613
    move-result-object v0

    .line 117440614
    invoke-direct {v3, v0}, Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;-><init>(Landroid/os/Looper;)V

    .line 117440615
    .line 117440616
    .line 117440617
    iput-object v3, v1, LX/08Vk;->LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 117440618
    .line 117440619
    if-gez v9, :cond_76

    .line 117440620
    .line 117440621
    new-instance v0, LX/08W2;

    .line 117440622
    .line 117440623
    invoke-direct {v0, v1}, LX/08W2;-><init>(LX/08Vk;)V

    .line 117440624
    .line 117440625
    .line 117440626
    invoke-static {v0}, LX/02ph;->LIZIZ(Lkotlin/jvm/functions/Function0;)LX/02pj;

    .line 117440627
    .line 117440628
    .line 117440629
    move-result-object v8

    .line 117440630
    :cond_76
    iput-object v8, v1, LX/08Vk;->LLJLL:LX/02pj;

    .line 117440631
    .line 117440632
    new-instance v0, Ljava/util/ArrayList;

    .line 117440633
    .line 117440634
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 117440635
    .line 117440636
    .line 117440637
    iput-object v0, v1, LX/08Vk;->LLJLLL:Ljava/util/List;

    .line 117440638
    .line 117440639
    new-instance v0, Ljava/util/ArrayList;

    .line 117440640
    .line 117440641
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    .line 117440642
    .line 117440643
    .line 117440644
    const/4 v10, 0x1

    .line 117440645
    iput v10, v1, LX/08Vk;->LLJZ:I

    .line 117440646
    .line 117440647
    const-string v0, ""

    .line 117440648
    .line 117440649
    iput-object v0, v1, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 117440650
    .line 117440651
    iput-object v0, v1, LX/08Vk;->LLLIIII:Ljava/lang/String;

    .line 117440652
    .line 117440653
    const/4 v0, -0x1

    .line 117440654
    iput v0, v1, LX/08Vk;->LLLIIIIL:I

    .line 117440655
    .line 117440656
    iput-boolean v10, v1, LX/08Vk;->LLLIILIL:Z

    .line 117440657
    .line 117440658
    iput-boolean v10, v1, LX/08Vk;->LLLILZ:Z

    .line 117440659
    .line 117440660
    sget-object v3, LX/08W5;->LJFF:LX/08WC;

    .line 117440661
    .line 117440662
    sget-object v0, LX/08WC;->DISABLE:LX/08WC;

    .line 117440663
    .line 117440664
    const/4 v11, 0x0

    .line 117440665
    if-eq v3, v0, :cond_1c5

    .line 117440666
    .line 117440667
    const/4 v0, 0x1

    .line 117440668
    :goto_9c
    iput-boolean v0, v1, LX/08Vk;->LLLLIIL:Z

    .line 117440669
    .line 117440670
    iput-boolean v10, v1, LX/08Vk;->LLLLIL:Z

    .line 117440671
    .line 117440672
    const/16 v0, 0x8

    .line 117440673
    .line 117440674
    invoke-static {v5, v0}, LX/00kD;->e0(LX/08Ic;I)V

    .line 117440675
    .line 117440676
    .line 117440677
    invoke-virtual {v5, v1}, LX/08Ic;->setOnSeekBarChangeListener(LX/08Vj;)V

    .line 117440678
    .line 117440679
    .line 117440680
    invoke-static {}, Lorg/greenrobot/eventbus/EventBus;->LIZLLL()Lorg/greenrobot/eventbus/EventBus;

    .line 117440681
    .line 117440682
    .line 117440683
    move-result-object v0

    .line 117440684
    invoke-virtual {v0, v1}, Lorg/greenrobot/eventbus/EventBus;->LJIJJ(Ljava/lang/Object;)V

    .line 117440685
    .line 117440686
    .line 117440687
    new-instance v9, LX/08WY;

    .line 117440688
    .line 117440689
    const/4 v0, 0x7

    .line 117440690
    new-array v8, v0, [LX/08Wt;

    .line 117440691
    .line 117440692
    new-instance v3, LX/08Wt;

    .line 117440693
    .line 117440694
    invoke-direct {v3, v12}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440695
    .line 117440696
    .line 117440697
    new-instance v0, LX/08Wv;

    .line 117440698
    .line 117440699
    invoke-direct {v0, v1}, LX/08Wv;-><init>(LX/08Vk;)V

    .line 117440700
    .line 117440701
    .line 117440702
    invoke-virtual {v0, v3}, LX/08Wv;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    .line 117440703
    .line 117440704
    .line 117440705
    aput-object v3, v8, v11

    .line 117440706
    .line 117440707
    new-instance v0, LX/08Wt;

    .line 117440708
    .line 117440709
    invoke-direct {v0, v4}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440710
    .line 117440711
    .line 117440712
    aput-object v0, v8, v10

    .line 117440713
    .line 117440714
    new-instance v3, LX/08Wt;

    .line 117440715
    .line 117440716
    const v0, 0x7f0a1ef4

    .line 117440717
    .line 117440718
    .line 117440719
    invoke-virtual {v4, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 117440720
    .line 117440721
    .line 117440722
    move-result-object v0

    .line 117440723
    invoke-direct {v3, v0}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440724
    .line 117440725
    .line 117440726
    new-instance v0, LX/08X1;

    .line 117440727
    .line 117440728
    invoke-direct {v0, v1}, LX/08X1;-><init>(LX/08Vk;)V

    .line 117440729
    .line 117440730
    .line 117440731
    invoke-virtual {v0, v3}, LX/08X1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    .line 117440732
    .line 117440733
    .line 117440734
    const/4 v0, 0x2

    .line 117440735
    aput-object v3, v8, v0

    .line 117440736
    .line 117440737
    new-instance v3, LX/08Wt;

    .line 117440738
    .line 117440739
    invoke-direct {v3, v6}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440740
    .line 117440741
    .line 117440742
    new-instance v0, LX/08X4;

    .line 117440743
    .line 117440744
    invoke-direct {v0}, LX/08X4;-><init>()V

    .line 117440745
    .line 117440746
    .line 117440747
    invoke-virtual {v0, v3}, LX/08X4;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    .line 117440748
    .line 117440749
    .line 117440750
    const/4 v0, 0x3

    .line 117440751
    aput-object v3, v8, v0

    .line 117440752
    .line 117440753
    new-instance v3, LX/08Wt;

    .line 117440754
    .line 117440755
    const v0, 0x7f0a8c22

    .line 117440756
    .line 117440757
    .line 117440758
    invoke-virtual {v4, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 117440759
    .line 117440760
    .line 117440761
    move-result-object v0

    .line 117440762
    invoke-direct {v3, v0}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440763
    .line 117440764
    .line 117440765
    new-instance v0, LX/08X6;

    .line 117440766
    .line 117440767
    invoke-direct {v0, v1}, LX/08X6;-><init>(LX/08Vk;)V

    .line 117440768
    .line 117440769
    .line 117440770
    invoke-virtual {v0, v3}, LX/08X6;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    .line 117440771
    .line 117440772
    .line 117440773
    const/4 v0, 0x4

    .line 117440774
    aput-object v3, v8, v0

    .line 117440775
    .line 117440776
    new-instance v6, LX/08Wt;

    .line 117440777
    .line 117440778
    const v3, 0x7f0a1ef5

    .line 117440779
    .line 117440780
    .line 117440781
    invoke-virtual {v4, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 117440782
    .line 117440783
    .line 117440784
    move-result-object v0

    .line 117440785
    invoke-direct {v6, v0}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440786
    .line 117440787
    .line 117440788
    new-instance v0, LX/08X8;

    .line 117440789
    .line 117440790
    invoke-direct {v0, v1}, LX/08X8;-><init>(LX/08Vk;)V

    .line 117440791
    .line 117440792
    .line 117440793
    invoke-virtual {v0, v6}, LX/08X8;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    .line 117440794
    .line 117440795
    .line 117440796
    const/4 v0, 0x5

    .line 117440797
    aput-object v6, v8, v0

    .line 117440798
    .line 117440799
    new-instance v6, LX/08Wt;

    .line 117440800
    .line 117440801
    invoke-direct {v6, v2}, LX/08Wt;-><init>(Landroid/view/View;)V

    .line 117440802
    .line 117440803
    .line 117440804
    new-instance v0, LX/08XB;

    .line 117440805
    .line 117440806
    invoke-direct {v0, v1}, LX/08XB;-><init>(LX/08Vk;)V

    .line 117440807
    .line 117440808
    .line 117440809
    invoke-virtual {v0, v6}, LX/08XB;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    .line 117440810
    .line 117440811
    .line 117440812
    const/4 v0, 0x6

    .line 117440813
    aput-object v6, v8, v0

    .line 117440814
    .line 117440815
    invoke-direct {v9, v8}, LX/08WY;-><init>([LX/08Wt;)V

    .line 117440816
    .line 117440817
    .line 117440818
    iput-object v9, v1, LX/08Vk;->LLLLIILL:LX/08WY;

    .line 117440819
    .line 117440820
    invoke-virtual {v1}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 117440821
    .line 117440822
    .line 117440823
    move-result-object v0

    .line 117440824
    invoke-virtual {v0}, LX/08WY;->LIZIZ()V

    .line 117440825
    .line 117440826
    .line 117440827
    sget-object v0, LX/08XF;->LIZ:LX/08XF;

    .line 117440828
    .line 117440829
    if-eqz v0, :cond_148

    .line 117440830
    .line 117440831
    invoke-virtual {v0}, LX/08XF;->LJIIJ()Lcom/ss/android/ugc/aweme/casting/api/ICastingPlayerService;

    .line 117440832
    .line 117440833
    .line 117440834
    move-result-object v0

    .line 117440835
    if-eqz v0, :cond_148

    .line 117440836
    .line 117440837
    invoke-interface {v0, v1}, Lcom/ss/android/ugc/aweme/casting/api/ICastingPlayerService;->addSeekListener(Lcom/ss/android/ugc/aweme/casting/api/ICastingSeekListener;)V

    .line 117440838
    .line 117440839
    .line 117440840
    :cond_148
    const-string/jumbo v0, "video seekbar has been created"

    .line 117440841
    .line 117440842
    .line 117440843
    invoke-virtual {v7, v0}, LX/05f6;->LJIIJ(Ljava/lang/String;)V

    .line 117440844
    .line 117440845
    .line 117440846
    const/16 v0, 0xc

    .line 117440847
    .line 117440848
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 117440849
    .line 117440850
    .line 117440851
    move-result-object v0

    .line 117440852
    invoke-static {v0}, LX/04J6;->LIZIZ(Ljava/lang/Number;)F

    .line 117440853
    .line 117440854
    .line 117440855
    move-result v0

    .line 117440856
    invoke-static {v0}, LX/06EZ;->LIZJ(F)I

    .line 117440857
    .line 117440858
    .line 117440859
    move-result v0

    .line 117440860
    invoke-virtual {v5, v0}, LX/08Ic;->setHorizontalMargin(I)V

    .line 117440861
    .line 117440862
    .line 117440863
    invoke-virtual {v4, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    .line 117440864
    .line 117440865
    .line 117440866
    move-result-object v3

    .line 117440867
    const/4 v4, 0x0

    .line 117440868
    invoke-virtual {v1}, LX/08Vk;->LJIJJLI()I

    .line 117440869
    .line 117440870
    .line 117440871
    move-result v0

    .line 117440872
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 117440873
    .line 117440874
    .line 117440875
    move-result-object v7

    .line 117440876
    const/4 v8, 0x0

    .line 117440877
    const/16 v9, 0x17

    .line 117440878
    .line 117440879
    move-object v5, v4

    .line 117440880
    move-object v6, v4

    .line 117440881
    invoke-static/range {v3 .. v9}, LX/04tC;->LJIIIZ(Landroid/view/View;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;ZI)V

    .line 117440882
    .line 117440883
    .line 117440884
    invoke-virtual {v1}, LX/08Vk;->LJIJJ()I

    .line 117440885
    .line 117440886
    .line 117440887
    move-result v0

    .line 117440888
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 117440889
    .line 117440890
    .line 117440891
    move-result-object v16

    .line 117440892
    move-object v13, v4

    .line 117440893
    move-object v14, v4

    .line 117440894
    move-object v15, v4

    .line 117440895
    move/from16 v17, v8

    .line 117440896
    .line 117440897
    move/from16 v18, v9

    .line 117440898
    .line 117440899
    invoke-static/range {v12 .. v18}, LX/04tC;->LJIIIZ(Landroid/view/View;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;ZI)V

    .line 117440900
    .line 117440901
    .line 117440902
    invoke-virtual {v1}, LX/08Vk;->LJIILJJIL()Z

    .line 117440903
    .line 117440904
    .line 117440905
    move-result v0

    .line 117440906
    if-eqz v0, :cond_1a7

    .line 117440907
    .line 117440908
    if-eqz v2, :cond_19f

    .line 117440909
    .line 117440910
    invoke-virtual {v1}, LX/08Vk;->LJIIZILJ()I

    .line 117440911
    .line 117440912
    .line 117440913
    move-result v0

    .line 117440914
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 117440915
    .line 117440916
    .line 117440917
    move-result-object v6

    .line 117440918
    move-object v3, v4

    .line 117440919
    move-object v4, v4

    .line 117440920
    move-object v5, v4

    .line 117440921
    move v7, v8

    .line 117440922
    move v8, v9

    .line 117440923
    move-object v2, v2

    .line 117440924
    invoke-static/range {v2 .. v8}, LX/04tC;->LJIIIZ(Landroid/view/View;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;ZI)V

    .line 117440925
    .line 117440926
    .line 117440927
    :cond_19f
    :goto_19f
    new-instance v0, LX/08Xl;

    .line 117440928
    .line 117440929
    invoke-direct {v0, v1}, LX/08Xl;-><init>(LX/08Vk;)V

    .line 117440930
    .line 117440931
    .line 117440932
    iput-object v0, v1, LX/08Vk;->LLLLILI:LX/08Xl;

    .line 117440933
    .line 117440934
    return-void

    .line 117440935
    :cond_1a7
    if-eqz v2, :cond_19f

    .line 117440936
    .line 117440937
    const/16 v0, 0x94

    .line 117440938
    .line 117440939
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 117440940
    .line 117440941
    .line 117440942
    move-result-object v0

    .line 117440943
    invoke-static {v0}, LX/04J6;->LIZIZ(Ljava/lang/Number;)F

    .line 117440944
    .line 117440945
    .line 117440946
    move-result v0

    .line 117440947
    invoke-static {v0}, LX/06EZ;->LIZJ(F)I

    .line 117440948
    .line 117440949
    .line 117440950
    move-result v0

    .line 117440951
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 117440952
    .line 117440953
    .line 117440954
    move-result-object v6

    .line 117440955
    move-object v3, v4

    .line 117440956
    move-object v4, v4

    .line 117440957
    move-object v5, v4

    .line 117440958
    move v7, v8

    .line 117440959
    move v8, v9

    .line 117440960
    move-object v2, v2

    .line 117440961
    invoke-static/range {v2 .. v8}, LX/04tC;->LJIIIZ(Landroid/view/View;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;ZI)V

    .line 117440962
    .line 117440963
    .line 117440964
    goto :goto_19f

    .line 117440965
    :cond_1c5
    const/4 v0, 0x0

    .line 117440966
    goto/16 :goto_9c

    .line 117440967
    .line 117440968
    :cond_1c8
    move-object v0, v8

    .line 117440969
    goto/16 :goto_54

    .line 117440970
    .line 117440971
    :cond_1cb
    move-object v0, v8

    .line 117440972
    goto/16 :goto_47

    .line 117440973
    .line 117440974
    :cond_1ce
    move-object v0, v8

    .line 117440975
    goto/16 :goto_3a
.end method

.method public static LJIIIIZZ(I)I
    .registers 3

    .prologue
    .line 16777216
    const/16 v0, 0xe10

    .line 16777217
    .line 16777218
    if-ge p0, v0, :cond_c

    .line 16777219
    .line 16777220
    div-int/lit8 v0, p0, 0x3c

    .line 16777221
    .line 16777222
    mul-int/lit8 v1, v0, 0x3c

    .line 16777223
    .line 16777224
    rem-int/lit8 v0, p0, 0x3c

    .line 16777225
    .line 16777226
    add-int/2addr v1, v0

    .line 16777227
    return v1

    .line 16777228
    :cond_c
    div-int/lit16 v1, p0, 0xe10

    .line 16777229
    .line 16777230
    rem-int/2addr p0, v0

    .line 16777231
    mul-int/2addr v1, v0

    .line 16777232
    invoke-static {p0}, LX/08Vk;->LJIIIIZZ(I)I

    .line 16777233
    .line 16777234
    .line 16777235
    move-result v0

    .line 16777236
    add-int/2addr v1, v0

    .line 16777237
    return v1
.end method


# virtual methods
.method public LIZ(FZ)V
    .registers 17

    .prologue
    .line 33554432
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554433
    .line 33554434
    const/4 v10, 0x2

    .line 33554435
    invoke-virtual {v0, v10}, LX/08Ic;->setSeekBarShowType(I)V

    .line 33554436
    .line 33554437
    .line 33554438
    const/4 v0, 0x0

    .line 33554439
    iput-boolean v0, p0, LX/08Vk;->LLLIILIL:Z

    .line 33554440
    .line 33554441
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554442
    .line 33554443
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 33554444
    .line 33554445
    .line 33554446
    move-result-object v0

    .line 33554447
    invoke-static {v0}, LX/06Tr;->LJIIJ(Landroid/content/Context;)I

    .line 33554448
    .line 33554449
    .line 33554450
    move-result v1

    .line 33554451
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554452
    .line 33554453
    invoke-virtual {v0}, LX/08Ic;->getMProgress()Ljava/lang/Float;

    .line 33554454
    .line 33554455
    .line 33554456
    move-result-object v0

    .line 33554457
    const/4 v6, 0x0

    .line 33554458
    if-eqz v0, :cond_179

    .line 33554459
    .line 33554460
    invoke-virtual {v0}, Ljava/lang/Float;->floatValue()F

    .line 33554461
    .line 33554462
    .line 33554463
    move-result v4

    .line 33554464
    :goto_20
    const/high16 v13, 0x42c80000    # 100.0f

    .line 33554465
    .line 33554466
    div-float v12, v4, v13

    .line 33554467
    .line 33554468
    int-to-float v8, v1

    .line 33554469
    mul-float/2addr v12, v8

    .line 33554470
    iget v7, p0, LX/08Vk;->LLJLIL:I

    .line 33554471
    .line 33554472
    sget v9, LX/0R2u;->LIZ:I

    .line 33554473
    .line 33554474
    const/4 v1, 0x3

    .line 33554475
    const/high16 v5, 0x3f800000    # 1.0f

    .line 33554476
    .line 33554477
    const/4 v3, 0x1

    .line 33554478
    if-eq v9, v3, :cond_32

    .line 33554479
    .line 33554480
    if-ne v9, v1, :cond_175

    .line 33554481
    .line 33554482
    :cond_32
    sget-object v11, LX/0T90;->LIZ:LX/02pj;

    .line 33554483
    .line 33554484
    invoke-interface {v11}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 33554485
    .line 33554486
    .line 33554487
    move-result-object v0

    .line 33554488
    check-cast v0, Ljava/lang/Number;

    .line 33554489
    .line 33554490
    invoke-virtual {v0}, Ljava/lang/Number;->floatValue()F

    .line 33554491
    .line 33554492
    .line 33554493
    move-result v0

    .line 33554494
    cmpl-float v0, v0, v6

    .line 33554495
    .line 33554496
    if-lez v0, :cond_170

    .line 33554497
    .line 33554498
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554499
    .line 33554500
    invoke-virtual {v0}, LX/08Ic;->getMutableSeekBar()LX/08Io;

    .line 33554501
    .line 33554502
    .line 33554503
    move-result-object v0

    .line 33554504
    if-eqz v0, :cond_16c

    .line 33554505
    .line 33554506
    invoke-virtual {v0}, Landroid/view/View;->getWidth()I

    .line 33554507
    .line 33554508
    .line 33554509
    move-result v0

    .line 33554510
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 33554511
    .line 33554512
    .line 33554513
    move-result-object v2

    .line 33554514
    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    .line 33554515
    .line 33554516
    .line 33554517
    move-result v0

    .line 33554518
    if-lez v0, :cond_16c

    .line 33554519
    .line 33554520
    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    .line 33554521
    .line 33554522
    .line 33554523
    move-result v7

    .line 33554524
    :goto_5c
    invoke-interface {v11}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 33554525
    .line 33554526
    .line 33554527
    move-result-object v0

    .line 33554528
    check-cast v0, Ljava/lang/Number;

    .line 33554529
    .line 33554530
    invoke-virtual {v0}, Ljava/lang/Number;->floatValue()F

    .line 33554531
    .line 33554532
    .line 33554533
    move-result v0

    .line 33554534
    cmpg-float v0, v0, v6

    .line 33554535
    .line 33554536
    if-lez v0, :cond_175

    .line 33554537
    .line 33554538
    invoke-interface {v11}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 33554539
    .line 33554540
    .line 33554541
    move-result-object v0

    .line 33554542
    check-cast v0, Ljava/lang/Number;

    .line 33554543
    .line 33554544
    invoke-virtual {v0}, Ljava/lang/Number;->floatValue()F

    .line 33554545
    .line 33554546
    .line 33554547
    move-result v2

    .line 33554548
    :goto_74
    if-eq v9, v10, :cond_78

    .line 33554549
    .line 33554550
    if-ne v9, v1, :cond_8f

    .line 33554551
    .line 33554552
    :cond_78
    const v1, 0x3dcccccd    # 0.1f

    .line 33554553
    .line 33554554
    .line 33554555
    mul-float/2addr v1, v8

    .line 33554556
    cmpg-float v0, v12, v1

    .line 33554557
    .line 33554558
    if-gtz v0, :cond_84

    .line 33554559
    .line 33554560
    cmpg-float v0, p1, v6

    .line 33554561
    .line 33554562
    if-ltz v0, :cond_8d

    .line 33554563
    .line 33554564
    :cond_84
    sub-float/2addr v8, v1

    .line 33554565
    cmpl-float v0, v12, v8

    .line 33554566
    .line 33554567
    if-ltz v0, :cond_8f

    .line 33554568
    .line 33554569
    cmpl-float v0, p1, v6

    .line 33554570
    .line 33554571
    if-lez v0, :cond_8f

    .line 33554572
    .line 33554573
    :cond_8d
    const/high16 v2, 0x40800000    # 4.0f

    .line 33554574
    .line 33554575
    :cond_8f
    int-to-float v0, v7

    .line 33554576
    div-float/2addr p1, v0

    .line 33554577
    mul-float/2addr p1, v13

    .line 33554578
    mul-float/2addr p1, v2

    .line 33554579
    add-float/2addr v4, p1

    .line 33554580
    cmpg-float v0, v4, v6

    .line 33554581
    .line 33554582
    if-gez v0, :cond_164

    .line 33554583
    .line 33554584
    const/4 v4, 0x0

    .line 33554585
    :cond_99
    :goto_99
    iput-boolean v3, p0, LX/08Vk;->LLLFF:Z

    .line 33554586
    .line 33554587
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554588
    .line 33554589
    invoke-virtual {v0, v4}, LX/08Ic;->setProgress(F)V

    .line 33554590
    .line 33554591
    .line 33554592
    invoke-static {}, LX/08Xg;->LIZ()Z

    .line 33554593
    .line 33554594
    .line 33554595
    move-result v0

    .line 33554596
    if-eqz v0, :cond_160

    .line 33554597
    .line 33554598
    const-wide/16 v1, 0x12c

    .line 33554599
    .line 33554600
    :goto_a8
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 33554601
    .line 33554602
    invoke-static {v0}, LX/08Xq;->LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 33554603
    .line 33554604
    .line 33554605
    move-result v0

    .line 33554606
    if-eqz v0, :cond_13a

    .line 33554607
    .line 33554608
    iget-object v0, p0, LX/08Vk;->LLJJIJIIJIL:Landroid/view/ViewGroup;

    .line 33554609
    .line 33554610
    invoke-virtual {v0}, Landroid/view/View;->getVisibility()I

    .line 33554611
    .line 33554612
    .line 33554613
    move-result v0

    .line 33554614
    if-eqz v0, :cond_13a

    .line 33554615
    .line 33554616
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554617
    .line 33554618
    invoke-virtual {v0}, Landroid/view/View;->getVisibility()I

    .line 33554619
    .line 33554620
    .line 33554621
    move-result v0

    .line 33554622
    if-nez v0, :cond_13a

    .line 33554623
    .line 33554624
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 33554625
    .line 33554626
    .line 33554627
    move-result-object v0

    .line 33554628
    invoke-virtual {v0}, LX/08WY;->LIZJ()V

    .line 33554629
    .line 33554630
    .line 33554631
    iget-object v0, p0, LX/08Vk;->LLJJIJIIJIL:Landroid/view/ViewGroup;

    .line 33554632
    .line 33554633
    invoke-static {v6, v0}, LX/00kD;->U4(FLandroid/view/ViewGroup;)V

    .line 33554634
    .line 33554635
    .line 33554636
    iget-object v0, p0, LX/08Vk;->LLJJIJIIJIL:Landroid/view/ViewGroup;

    .line 33554637
    .line 33554638
    invoke-virtual {v0}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 33554639
    .line 33554640
    .line 33554641
    move-result-object v0

    .line 33554642
    if-eqz v0, :cond_e3

    .line 33554643
    .line 33554644
    invoke-virtual {v0, v5}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 33554645
    .line 33554646
    .line 33554647
    move-result-object v0

    .line 33554648
    if-eqz v0, :cond_e3

    .line 33554649
    .line 33554650
    invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 33554651
    .line 33554652
    .line 33554653
    move-result-object v0

    .line 33554654
    if-eqz v0, :cond_e3

    .line 33554655
    .line 33554656
    invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 33554657
    .line 33554658
    .line 33554659
    :cond_e3
    iget-object v0, p0, LX/08Vk;->LLJJJ:Landroid/widget/ImageView;

    .line 33554660
    .line 33554661
    invoke-static {v0, v6}, LX/00kD;->Y4(Landroid/widget/ImageView;F)V

    .line 33554662
    .line 33554663
    .line 33554664
    iget-object v0, p0, LX/08Vk;->LLJJJ:Landroid/widget/ImageView;

    .line 33554665
    .line 33554666
    invoke-virtual {v0}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 33554667
    .line 33554668
    .line 33554669
    move-result-object v0

    .line 33554670
    if-eqz v0, :cond_ff

    .line 33554671
    .line 33554672
    invoke-virtual {v0, v5}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 33554673
    .line 33554674
    .line 33554675
    move-result-object v0

    .line 33554676
    if-eqz v0, :cond_ff

    .line 33554677
    .line 33554678
    invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 33554679
    .line 33554680
    .line 33554681
    move-result-object v0

    .line 33554682
    if-eqz v0, :cond_ff

    .line 33554683
    .line 33554684
    invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 33554685
    .line 33554686
    .line 33554687
    :cond_ff
    iget-object v0, p0, LX/08Vk;->LLJJIJIL:Landroid/view/View;

    .line 33554688
    .line 33554689
    invoke-static {v0, v6}, LX/00kD;->W4(Landroid/view/View;F)V

    .line 33554690
    .line 33554691
    .line 33554692
    iget-object v0, p0, LX/08Vk;->LLJJIJIL:Landroid/view/View;

    .line 33554693
    .line 33554694
    invoke-virtual {v0}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 33554695
    .line 33554696
    .line 33554697
    move-result-object v0

    .line 33554698
    if-eqz v0, :cond_11b

    .line 33554699
    .line 33554700
    invoke-virtual {v0, v5}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 33554701
    .line 33554702
    .line 33554703
    move-result-object v0

    .line 33554704
    if-eqz v0, :cond_11b

    .line 33554705
    .line 33554706
    invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 33554707
    .line 33554708
    .line 33554709
    move-result-object v0

    .line 33554710
    if-eqz v0, :cond_11b

    .line 33554711
    .line 33554712
    invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 33554713
    .line 33554714
    .line 33554715
    :cond_11b
    iget-object v0, p0, LX/08Vk;->LLJJJJJIL:LX/08WH;

    .line 33554716
    .line 33554717
    if-eqz v0, :cond_13a

    .line 33554718
    .line 33554719
    invoke-static {v0}, LX/06w5;->LIZJ(Ljava/lang/Object;)V

    .line 33554720
    .line 33554721
    .line 33554722
    invoke-virtual {v0, v6}, Landroid/view/View;->setAlpha(F)V

    .line 33554723
    .line 33554724
    .line 33554725
    invoke-virtual {v0}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 33554726
    .line 33554727
    .line 33554728
    move-result-object v0

    .line 33554729
    if-eqz v0, :cond_13a

    .line 33554730
    .line 33554731
    invoke-virtual {v0, v5}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 33554732
    .line 33554733
    .line 33554734
    move-result-object v0

    .line 33554735
    if-eqz v0, :cond_13a

    .line 33554736
    .line 33554737
    invoke-virtual {v0, v1, v2}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 33554738
    .line 33554739
    .line 33554740
    move-result-object v0

    .line 33554741
    if-eqz v0, :cond_13a

    .line 33554742
    .line 33554743
    invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 33554744
    .line 33554745
    .line 33554746
    :cond_13a
    iget-boolean v0, p0, LX/08Vk;->LLLF:Z

    .line 33554747
    .line 33554748
    if-nez v0, :cond_147

    .line 33554749
    .line 33554750
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554751
    .line 33554752
    invoke-virtual {v0}, LX/08Ic;->getMutableSeekBar()LX/08Io;

    .line 33554753
    .line 33554754
    .line 33554755
    move-result-object v0

    .line 33554756
    invoke-virtual {p0, v0}, LX/08Vk;->onStartTrackingTouch(Landroid/widget/SeekBar;)V

    .line 33554757
    .line 33554758
    .line 33554759
    :cond_147
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554760
    .line 33554761
    invoke-virtual {v0}, LX/08Ic;->getMutableSeekBar()LX/08Io;

    .line 33554762
    .line 33554763
    .line 33554764
    move-result-object v1

    .line 33554765
    mul-float/2addr v4, v13

    .line 33554766
    float-to-int v0, v4

    .line 33554767
    invoke-virtual {p0, v1, v0, v3}, LX/08Vk;->onProgressChanged(Landroid/widget/SeekBar;IZ)V

    .line 33554768
    .line 33554769
    .line 33554770
    if-eqz p2, :cond_15f

    .line 33554771
    .line 33554772
    iput-boolean v3, p0, LX/08Vk;->LLLIILIL:Z

    .line 33554773
    .line 33554774
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 33554775
    .line 33554776
    invoke-virtual {v0}, LX/08Ic;->getMutableSeekBar()LX/08Io;

    .line 33554777
    .line 33554778
    .line 33554779
    move-result-object v0

    .line 33554780
    invoke-virtual {p0, v0}, LX/08Vk;->onStopTrackingTouch(Landroid/widget/SeekBar;)V

    .line 33554781
    .line 33554782
    .line 33554783
    :cond_15f
    return-void

    .line 33554784
    :cond_160
    const-wide/16 v1, 0x64

    .line 33554785
    .line 33554786
    goto/16 :goto_a8

    .line 33554787
    .line 33554788
    :cond_164
    cmpl-float v0, v4, v13

    .line 33554789
    .line 33554790
    if-lez v0, :cond_99

    .line 33554791
    .line 33554792
    const/high16 v4, 0x42c80000    # 100.0f

    .line 33554793
    .line 33554794
    goto/16 :goto_99

    .line 33554795
    .line 33554796
    :cond_16c
    iget v7, p0, LX/08Vk;->LLJLIL:I

    .line 33554797
    .line 33554798
    goto/16 :goto_5c

    .line 33554799
    .line 33554800
    :cond_170
    const v2, 0x3fb33333    # 1.4f

    .line 33554801
    .line 33554802
    .line 33554803
    goto/16 :goto_74

    .line 33554804
    .line 33554805
    :cond_175
    const/high16 v2, 0x3f800000    # 1.0f

    .line 33554806
    .line 33554807
    goto/16 :goto_74

    .line 33554808
    .line 33554809
    :cond_179
    const/4 v4, 0x0

    .line 33554810
    goto/16 :goto_20
.end method

.method public final LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;JJLjava/lang/String;)V
    .registers 14

    .prologue
    .line 67108864
    const/4 v1, 0x1

    .line 67108865
    long-to-float v4, p4

    .line 67108866
    long-to-float v0, p2

    .line 67108867
    div-float/2addr v4, v0

    .line 67108868
    const/16 v0, 0x64

    .line 67108869
    .line 67108870
    int-to-float v0, v0

    .line 67108871
    mul-float/2addr v4, v0

    .line 67108872
    sget-object v6, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    .line 67108873
    .line 67108874
    move-object v5, p6

    .line 67108875
    move-object v3, p1

    .line 67108876
    move-object v0, p0

    .line 67108877
    move v2, v1

    .line 67108878
    invoke-virtual/range {v0 .. v6}, LX/08Vk;->LJIL(ZZLcom/ss/android/ugc/aweme/feed/model/Aweme;FLjava/lang/String;Ljava/lang/Boolean;)V

    .line 67108879
    .line 67108880
    .line 67108881
    return-void
.end method

.method public LIZJ()V
    .registers 1

    return-void
.end method

.method public final LIZLLL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V
    .registers 3

    .prologue
    .line 16777216
    if-eqz p1, :cond_b

    .line 16777217
    .line 16777218
    iput-object p1, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777219
    .line 16777220
    invoke-virtual {p0, p1}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777221
    .line 16777222
    .line 16777223
    const/4 v0, 0x1

    .line 16777224
    invoke-virtual {p0, v0}, LX/08Vk;->LJJI(Z)V

    .line 16777225
    .line 16777226
    .line 16777227
    :cond_b
    return-void
.end method

.method public LJ()V
    .registers 1

    return-void
.end method

.method public final LJFF(ILcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;Ljava/lang/String;)V
    .registers 6

    .prologue
    .line 67108864
    invoke-virtual {p0, p2}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 67108865
    .line 67108866
    .line 67108867
    iput-object p2, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 67108868
    .line 67108869
    iput-object p3, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 67108870
    .line 67108871
    iput p1, p0, LX/08Vk;->LLLIIIIL:I

    .line 67108872
    .line 67108873
    iput-object p4, p0, LX/08Vk;->LLLIIII:Ljava/lang/String;

    .line 67108874
    .line 67108875
    const/4 v0, 0x1

    .line 67108876
    invoke-virtual {p0, v0}, LX/08Vk;->LJJI(Z)V

    .line 67108877
    .line 67108878
    .line 67108879
    return-void
.end method

.method public final LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z
    .registers 7

    .prologue
    .line 16777216
    sget-object v4, LX/056z;->LIZIZ:LX/056z;

    .line 16777217
    .line 16777218
    invoke-virtual {v4}, LX/056z;->LJJIIZI()LX/09UL;

    .line 16777219
    .line 16777220
    .line 16777221
    move-result-object v1

    .line 16777222
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777223
    .line 16777224
    const/4 v2, 0x0

    .line 16777225
    if-eqz v0, :cond_1e

    .line 16777226
    .line 16777227
    invoke-interface {v0}, LX/05fP;->getFragment()Landroidx/fragment/app/Fragment;

    .line 16777228
    .line 16777229
    .line 16777230
    move-result-object v0

    .line 16777231
    :goto_f
    invoke-interface {v1, v0}, LX/09UL;->LJJIJIIJIL(Landroidx/fragment/app/Fragment;)Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    const/4 v3, 0x0

    .line 16777236
    if-eqz v0, :cond_20

    .line 16777237
    .line 16777238
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777239
    .line 16777240
    const-string v0, "can not show seekbar, in story immersiveFeed"

    .line 16777241
    .line 16777242
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777243
    .line 16777244
    .line 16777245
    return v3

    .line 16777246
    :cond_1e
    move-object v0, v2

    .line 16777247
    goto :goto_f

    .line 16777248
    :cond_20
    invoke-virtual {v4}, LX/056z;->LJJIIZI()LX/09UL;

    .line 16777249
    .line 16777250
    .line 16777251
    move-result-object v1

    .line 16777252
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777253
    .line 16777254
    if-eqz v0, :cond_2c

    .line 16777255
    .line 16777256
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->LJLJJLL()LX/08BV;

    .line 16777257
    .line 16777258
    .line 16777259
    move-result-object v2

    .line 16777260
    :cond_2c
    invoke-interface {v1, v2}, LX/09UL;->LJIJJ(Ljava/lang/Object;)Z

    .line 16777261
    .line 16777262
    .line 16777263
    move-result v0

    .line 16777264
    if-eqz v0, :cond_3a

    .line 16777265
    .line 16777266
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777267
    .line 16777268
    const-string v0, "can not show seekbar, in story guide card"

    .line 16777269
    .line 16777270
    invoke-virtual {v1, v0}, LX/05f6;->LJIIJ(Ljava/lang/String;)V

    .line 16777271
    .line 16777272
    .line 16777273
    return v3

    .line 16777274
    :cond_3a
    iget v0, p0, LX/08Vk;->LLJZ:I

    .line 16777275
    .line 16777276
    const/4 v2, 0x1

    .line 16777277
    if-eq v2, v0, :cond_47

    .line 16777278
    .line 16777279
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777280
    .line 16777281
    const-string v0, "can not show seekbar, state: 1, not in resume"

    .line 16777282
    .line 16777283
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777284
    .line 16777285
    .line 16777286
    return v3

    .line 16777287
    :cond_47
    invoke-static {p1}, LX/08Xq;->LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777288
    .line 16777289
    .line 16777290
    move-result v0

    .line 16777291
    if-nez v0, :cond_55

    .line 16777292
    .line 16777293
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777294
    .line 16777295
    const-string v0, "can not show seekbar, state: 2, can not drag"

    .line 16777296
    .line 16777297
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777298
    .line 16777299
    .line 16777300
    return v3

    .line 16777301
    :cond_55
    iget-boolean v0, p0, LX/08Vk;->LLLILZLLLI:Z

    .line 16777302
    .line 16777303
    if-nez v0, :cond_75

    .line 16777304
    .line 16777305
    sget-object v0, Lcom/ss/android/ugc/aweme/appdiff/ICommonFeedAppDiffProtocol;->LIZ:LX/05zy;

    .line 16777306
    .line 16777307
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777308
    .line 16777309
    .line 16777310
    invoke-static {}, LX/05zy;->LIZ()Lcom/ss/android/ugc/aweme/appdiff/ICommonFeedAppDiffProtocol;

    .line 16777311
    .line 16777312
    .line 16777313
    move-result-object v1

    .line 16777314
    new-instance v0, LX/0BHe;

    .line 16777315
    .line 16777316
    invoke-direct {v0, p0, p1}, LX/0BHe;-><init>(LX/08Vk;Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777317
    .line 16777318
    .line 16777319
    invoke-interface {v1, v0}, Lcom/ss/android/ugc/aweme/appdiff/ICommonFeedAppDiffProtocol;->LJIIIZ(LX/0BHe;)Z

    .line 16777320
    .line 16777321
    .line 16777322
    move-result v0

    .line 16777323
    if-nez v0, :cond_75

    .line 16777324
    .line 16777325
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777326
    .line 16777327
    const-string v0, "can not show seekbar, state: 3, not current aweme"

    .line 16777328
    .line 16777329
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777330
    .line 16777331
    .line 16777332
    return v3

    .line 16777333
    :cond_75
    invoke-static {p1}, LX/08Xq;->LIZJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777334
    .line 16777335
    .line 16777336
    move-result v0

    .line 16777337
    if-nez v0, :cond_83

    .line 16777338
    .line 16777339
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777340
    .line 16777341
    const-string v0, "can not show seekbar, state: 4, can not show progressbar"

    .line 16777342
    .line 16777343
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777344
    .line 16777345
    .line 16777346
    return v3

    .line 16777347
    :cond_83
    invoke-static {p1}, LX/0A0G;->LJJIJLIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777348
    .line 16777349
    .line 16777350
    move-result v0

    .line 16777351
    if-eqz v0, :cond_91

    .line 16777352
    .line 16777353
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777354
    .line 16777355
    const-string v0, "can not show seekbar, state: 5, is story video"

    .line 16777356
    .line 16777357
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777358
    .line 16777359
    .line 16777360
    return v3

    .line 16777361
    :cond_91
    invoke-static {p1}, LX/0BHi;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777362
    .line 16777363
    .line 16777364
    move-result v0

    .line 16777365
    if-nez v0, :cond_9f

    .line 16777366
    .line 16777367
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777368
    .line 16777369
    const-string v0, "can not show seekbar, state: 6, hit inverse expriment"

    .line 16777370
    .line 16777371
    invoke-virtual {v1, v0}, LX/05f6;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777372
    .line 16777373
    .line 16777374
    return v3

    .line 16777375
    :cond_9f
    invoke-static {p1}, LX/0AFV;->LJIIJJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777376
    .line 16777377
    .line 16777378
    move-result v0

    .line 16777379
    if-eqz v0, :cond_ad

    .line 16777380
    .line 16777381
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777382
    .line 16777383
    const-string v0, "can not show seekbar, state: 7, video has mask"

    .line 16777384
    .line 16777385
    invoke-virtual {v1, v0}, LX/05f6;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777386
    .line 16777387
    .line 16777388
    return v3

    .line 16777389
    :cond_ad
    sget-object v0, LX/06OD;->LIZIZ:LX/06OD;

    .line 16777390
    .line 16777391
    invoke-virtual {v0, p1}, LX/06OD;->LJJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777392
    .line 16777393
    .line 16777394
    move-result v0

    .line 16777395
    if-eqz v0, :cond_bd

    .line 16777396
    .line 16777397
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777398
    .line 16777399
    const-string v0, "can not show seekbar, state: 14, prompt overlay"

    .line 16777400
    .line 16777401
    invoke-virtual {v1, v0}, LX/05f6;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777402
    .line 16777403
    .line 16777404
    return v3

    .line 16777405
    :cond_bd
    if-eqz p1, :cond_cd

    .line 16777406
    .line 16777407
    invoke-static {p1}, Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;->isProhibitedAndShouldTell(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777408
    .line 16777409
    .line 16777410
    move-result v0

    .line 16777411
    if-ne v0, v2, :cond_cd

    .line 16777412
    .line 16777413
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777414
    .line 16777415
    const-string v0, "can not show seekbar, state: 10, video is prohibited"

    .line 16777416
    .line 16777417
    invoke-virtual {v1, v0}, LX/05f6;->LJIILLIIL(Ljava/lang/String;)V

    .line 16777418
    .line 16777419
    .line 16777420
    return v3

    .line 16777421
    :cond_cd
    return v2
.end method

.method public final LJII()Z
    .registers 6

    .prologue
    .line 0
    iget-boolean v0, p0, LX/08Vk;->LLLLIIL:Z

    .line 1
    .line 2
    const/4 v4, 0x0

    .line 3
    if-nez v0, :cond_6

    .line 4
    .line 5
    return v4

    .line 6
    :cond_6
    sget-object v0, LX/08XZ;->LIZ:LX/02pj;

    .line 7
    .line 8
    invoke-interface {v0}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 9
    .line 10
    .line 11
    move-result-object v0

    .line 12
    check-cast v0, Ljava/lang/Number;

    .line 13
    .line 14
    invoke-virtual {v0}, Ljava/lang/Number;->intValue()I

    .line 15
    .line 16
    .line 17
    move-result v0

    .line 18
    const/4 v3, 0x1

    .line 19
    if-eq v0, v3, :cond_38

    .line 20
    .line 21
    invoke-virtual {p0}, LX/08Vk;->LJIILJJIL()Z

    .line 22
    .line 23
    .line 24
    move-result v0

    .line 25
    if-nez v0, :cond_38

    .line 26
    .line 27
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 28
    .line 29
    if-eqz v0, :cond_3c

    .line 30
    .line 31
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 32
    .line 33
    .line 34
    move-result-object v0

    .line 35
    if-eqz v0, :cond_3c

    .line 36
    .line 37
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 38
    .line 39
    .line 40
    move-result v0

    .line 41
    int-to-long v1, v0

    .line 42
    :goto_2a
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 43
    .line 44
    if-eqz v0, :cond_3a

    .line 45
    .line 46
    invoke-interface {v0, v3}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 47
    .line 48
    .line 49
    move-result-object v0

    .line 50
    :goto_32
    invoke-static {v1, v2, v0}, LX/08Xg;->LIZIZ(JLjava/lang/String;)Z

    .line 51
    .line 52
    .line 53
    move-result v0

    .line 54
    if-eqz v0, :cond_39

    .line 55
    .line 56
    :cond_38
    const/4 v4, 0x1

    .line 57
    :cond_39
    return v4

    .line 58
    :cond_3a
    const/4 v0, 0x0

    .line 59
    goto :goto_32

    .line 60
    :cond_3c
    const-wide/16 v1, 0x0

    .line 61
    .line 62
    goto :goto_2a
.end method

.method public final LJIIIZ(I)I
    .registers 4

    .prologue
    .line 16777216
    int-to-float v1, p1

    .line 16777217
    const/high16 v0, 0x42c80000    # 100.0f

    .line 16777218
    .line 16777219
    div-float/2addr v1, v0

    .line 16777220
    iget v0, p0, LX/08Vk;->LLL:I

    .line 16777221
    .line 16777222
    int-to-float v0, v0

    .line 16777223
    mul-float/2addr v1, v0

    .line 16777224
    const/16 v0, 0x64

    .line 16777225
    .line 16777226
    int-to-float v0, v0

    .line 16777227
    div-float/2addr v1, v0

    .line 16777228
    float-to-int v0, v1

    .line 16777229
    return v0
.end method

.method public final LJIIJ()LX/08WY;
    .registers 2

    .prologue
    .line 0
    iget-object v0, p0, LX/08Vk;->LLLLIILL:LX/08WY;

    .line 1
    .line 2
    if-eqz v0, :cond_5

    .line 3
    .line 4
    return-object v0

    .line 5
    :cond_5
    const/4 v0, 0x0

    .line 6
    return-object v0
.end method

.method public final LJIIJJI(Z)I
    .registers 5

    .prologue
    .line 16777216
    iget v1, p0, LX/08Vk;->LLL:I

    .line 16777217
    .line 16777218
    const/16 v0, 0x1e

    .line 16777219
    .line 16777220
    const/4 v2, 0x0

    .line 16777221
    if-ge v1, v0, :cond_23

    .line 16777222
    .line 16777223
    sget-object v0, LX/0ADW;->LJ:LX/02pj;

    .line 16777224
    .line 16777225
    invoke-static {}, LX/0ADY;->LIZ()LX/0ADW;

    .line 16777226
    .line 16777227
    .line 16777228
    move-result-object v1

    .line 16777229
    iget-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777230
    .line 16777231
    invoke-virtual {v1, v0}, LX/0ADW;->LIZIZ(Ljava/lang/String;)Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    if-eqz v0, :cond_17

    .line 16777236
    .line 16777237
    if-eqz p1, :cond_23

    .line 16777238
    .line 16777239
    :cond_17
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777240
    .line 16777241
    if-eqz v0, :cond_24

    .line 16777242
    .line 16777243
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAwemeType()I

    .line 16777244
    .line 16777245
    .line 16777246
    move-result v1

    .line 16777247
    const/16 v0, 0x1f6

    .line 16777248
    .line 16777249
    if-ne v1, v0, :cond_24

    .line 16777250
    .line 16777251
    :cond_23
    return v2

    .line 16777252
    :cond_24
    const/4 v2, 0x4

    .line 16777253
    return v2
.end method

.method public LJIIL()Ljava/lang/String;
    .registers 2

    const-string v0, "CommonControl"

    return-object v0
.end method

.method public final LJIILIIL()Z
    .registers 2

    .prologue
    .line 0
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 1
    .line 2
    if-eqz v0, :cond_a

    .line 3
    .line 4
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->getPlayerManager()LX/03Yo;

    .line 5
    .line 6
    .line 7
    move-result-object v0

    .line 8
    if-nez v0, :cond_e

    .line 9
    .line 10
    :cond_a
    invoke-static {}, LX/03Yp;->LJIILIIL()LX/03Yo;

    .line 11
    .line 12
    .line 13
    move-result-object v0

    .line 14
    :cond_e
    invoke-interface {v0}, LX/03Ym;->isPaused()Z

    .line 15
    .line 16
    .line 17
    move-result v0

    .line 18
    return v0
.end method

.method public final LJIILJJIL()Z
    .registers 5

    .prologue
    .line 0
    sget-object v1, LX/08Xe;->LIZ:LX/08Xe;

    .line 1
    .line 2
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 3
    .line 4
    const/4 v3, 0x0

    .line 5
    const/4 v2, 0x1

    .line 6
    if-eqz v0, :cond_32

    .line 7
    .line 8
    invoke-interface {v0, v2}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 9
    .line 10
    .line 11
    move-result-object v0

    .line 12
    :goto_c
    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 13
    .line 14
    .line 15
    invoke-static {v0}, LX/08Xe;->LIZ(Ljava/lang/String;)Z

    .line 16
    .line 17
    .line 18
    move-result v0

    .line 19
    if-eqz v0, :cond_34

    .line 20
    .line 21
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 22
    .line 23
    .line 24
    move-result-object v1

    .line 25
    const-class v0, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;

    .line 26
    .line 27
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 28
    .line 29
    .line 30
    move-result-object v1

    .line 31
    check-cast v1, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;

    .line 32
    .line 33
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 34
    .line 35
    if-eqz v0, :cond_29

    .line 36
    .line 37
    invoke-interface {v0, v2}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 38
    .line 39
    .line 40
    move-result-object v3

    .line 41
    :cond_29
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 42
    .line 43
    invoke-interface {v1, v0, v3}, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;->LJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;)Z

    .line 44
    .line 45
    .line 46
    move-result v0

    .line 47
    if-eqz v0, :cond_34

    .line 48
    .line 49
    return v2

    .line 50
    :cond_32
    move-object v0, v3

    .line 51
    goto :goto_c

    .line 52
    :cond_34
    const/4 v2, 0x0

    .line 53
    return v2
.end method

.method public final LJIILL()Z
    .registers 5

    .prologue
    .line 0
    sget-object v1, LX/08Xe;->LIZ:LX/08Xe;

    .line 1
    .line 2
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 3
    .line 4
    const/4 v3, 0x0

    .line 5
    const/4 v2, 0x1

    .line 6
    if-eqz v0, :cond_32

    .line 7
    .line 8
    invoke-interface {v0, v2}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 9
    .line 10
    .line 11
    move-result-object v0

    .line 12
    :goto_c
    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 13
    .line 14
    .line 15
    invoke-static {v0}, LX/08Xe;->LIZ(Ljava/lang/String;)Z

    .line 16
    .line 17
    .line 18
    move-result v0

    .line 19
    if-eqz v0, :cond_34

    .line 20
    .line 21
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 22
    .line 23
    .line 24
    move-result-object v1

    .line 25
    const-class v0, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;

    .line 26
    .line 27
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 28
    .line 29
    .line 30
    move-result-object v1

    .line 31
    check-cast v1, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;

    .line 32
    .line 33
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 34
    .line 35
    if-eqz v0, :cond_29

    .line 36
    .line 37
    invoke-interface {v0, v2}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 38
    .line 39
    .line 40
    move-result-object v3

    .line 41
    :cond_29
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 42
    .line 43
    invoke-interface {v1, v0, v3}, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;->LJIILJJIL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;Ljava/lang/String;)Z

    .line 44
    .line 45
    .line 46
    move-result v0

    .line 47
    if-eqz v0, :cond_34

    .line 48
    .line 49
    return v2

    .line 50
    :cond_32
    move-object v0, v3

    .line 51
    goto :goto_c

    .line 52
    :cond_34
    const/4 v2, 0x0

    .line 53
    return v2
.end method

.method public LJIILLIIL(Ljava/lang/Object;)Z
    .registers 3

    const/4 v0, 0x1

    return v0
.end method

.method public final LJIIZILJ()I
    .registers 5

    .prologue
    .line 0
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 1
    .line 2
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 3
    .line 4
    .line 5
    move-result-object v1

    .line 6
    sget v3, LX/03bm;->LJIIJJI:I

    .line 7
    .line 8
    int-to-float v0, v3

    .line 9
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 10
    .line 11
    .line 12
    move-result v0

    .line 13
    float-to-int v2, v0

    .line 14
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 15
    .line 16
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 17
    .line 18
    .line 19
    move-result-object v1

    .line 20
    int-to-float v0, v3

    .line 21
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 22
    .line 23
    .line 24
    move-result v0

    .line 25
    float-to-int v3, v0

    .line 26
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 27
    .line 28
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 29
    .line 30
    .line 31
    move-result-object v1

    .line 32
    const/high16 v0, 0x42700000    # 60.0f

    .line 33
    .line 34
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 35
    .line 36
    .line 37
    move-result v0

    .line 38
    float-to-int v0, v0

    .line 39
    add-int/2addr v3, v0

    .line 40
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 41
    .line 42
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 43
    .line 44
    .line 45
    move-result-object v1

    .line 46
    const/high16 v0, 0x41f00000    # 30.0f

    .line 47
    .line 48
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 49
    .line 50
    .line 51
    move-result v0

    .line 52
    float-to-int v0, v0

    .line 53
    add-int/2addr v3, v0

    .line 54
    invoke-virtual {p0}, LX/08Vk;->LJIJI()Z

    .line 55
    .line 56
    .line 57
    move-result v0

    .line 58
    if-eqz v0, :cond_4b

    .line 59
    .line 60
    const/16 v0, 0x56

    .line 61
    .line 62
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 63
    .line 64
    .line 65
    move-result-object v0

    .line 66
    invoke-static {v0}, LX/04J6;->LIZIZ(Ljava/lang/Number;)F

    .line 67
    .line 68
    .line 69
    move-result v0

    .line 70
    invoke-static {v0}, LX/06EZ;->LIZJ(F)I

    .line 71
    .line 72
    .line 73
    move-result v3

    .line 74
    add-int/2addr v3, v2

    .line 75
    :cond_4b
    return v3
.end method

.method public final LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V
    .registers 5

    .prologue
    .line 16777216
    const/4 v2, 0x0

    .line 16777217
    if-eqz p1, :cond_27

    .line 16777218
    .line 16777219
    invoke-virtual {p1}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777220
    .line 16777221
    .line 16777222
    move-result-object v1

    .line 16777223
    :goto_7
    iget-object v0, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777224
    .line 16777225
    if-eqz v0, :cond_f

    .line 16777226
    .line 16777227
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777228
    .line 16777229
    .line 16777230
    move-result-object v2

    .line 16777231
    :cond_f
    invoke-static {v1, v2}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    if-nez v0, :cond_24

    .line 16777236
    .line 16777237
    iput-object p1, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777238
    .line 16777239
    sget-object v2, LX/08W5;->LJFF:LX/08WC;

    .line 16777240
    .line 16777241
    sget-object v0, LX/08WC;->DISABLE:LX/08WC;

    .line 16777242
    .line 16777243
    const/4 v1, 0x1

    .line 16777244
    if-eq v2, v0, :cond_25

    .line 16777245
    .line 16777246
    const/4 v0, 0x1

    .line 16777247
    :goto_1f
    iput-boolean v0, p0, LX/08Vk;->LLLLIIL:Z

    .line 16777248
    .line 16777249
    invoke-virtual {p0, v1}, LX/08Vk;->LJJI(Z)V

    .line 16777250
    .line 16777251
    .line 16777252
    :cond_24
    return-void

    .line 16777253
    :cond_25
    const/4 v0, 0x0

    .line 16777254
    goto :goto_1f

    .line 16777255
    :cond_27
    move-object v1, v2

    .line 16777256
    goto :goto_7
.end method

.method public final LJIJI()Z
    .registers 4

    .prologue
    .line 0
    invoke-virtual {p0}, LX/08Vk;->LJII()Z

    .line 1
    .line 2
    .line 3
    move-result v0

    .line 4
    const/4 v2, 0x0

    .line 5
    if-eqz v0, :cond_13

    .line 6
    .line 7
    iget-object v0, p0, LX/08Vk;->LLLLIIIILLL:LX/0Lxl;

    .line 8
    .line 9
    const/4 v1, 0x1

    .line 10
    if-eqz v0, :cond_13

    .line 11
    .line 12
    invoke-virtual {v0}, LX/0Lxk;->LIZ()Z

    .line 13
    .line 14
    .line 15
    move-result v0

    .line 16
    if-ne v0, v1, :cond_13

    .line 17
    .line 18
    const/4 v2, 0x1

    .line 19
    :cond_13
    return v2
.end method

.method public final LJIJJ()I
    .registers 5

    .prologue
    .line 0
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 1
    .line 2
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 3
    .line 4
    .line 5
    move-result-object v1

    .line 6
    sget v3, LX/03bm;->LJIIJJI:I

    .line 7
    .line 8
    int-to-float v0, v3

    .line 9
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 10
    .line 11
    .line 12
    move-result v0

    .line 13
    float-to-int v2, v0

    .line 14
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 15
    .line 16
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 17
    .line 18
    .line 19
    move-result-object v1

    .line 20
    int-to-float v0, v3

    .line 21
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 22
    .line 23
    .line 24
    move-result v3

    .line 25
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 26
    .line 27
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 28
    .line 29
    .line 30
    move-result-object v1

    .line 31
    const/high16 v0, 0x42700000    # 60.0f

    .line 32
    .line 33
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 34
    .line 35
    .line 36
    move-result v0

    .line 37
    add-float/2addr v3, v0

    .line 38
    float-to-int v3, v3

    .line 39
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 40
    .line 41
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 42
    .line 43
    .line 44
    move-result-object v1

    .line 45
    const/high16 v0, 0x42300000    # 44.0f

    .line 46
    .line 47
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 48
    .line 49
    .line 50
    move-result v0

    .line 51
    float-to-int v0, v0

    .line 52
    add-int/2addr v3, v0

    .line 53
    invoke-virtual {p0}, LX/08Vk;->LJIILJJIL()Z

    .line 54
    .line 55
    .line 56
    move-result v0

    .line 57
    if-eqz v0, :cond_68

    .line 58
    .line 59
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 60
    .line 61
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 62
    .line 63
    .line 64
    move-result-object v1

    .line 65
    const/high16 v0, 0x41300000    # 11.0f

    .line 66
    .line 67
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 68
    .line 69
    .line 70
    move-result v0

    .line 71
    float-to-int v0, v0

    .line 72
    :goto_48
    add-int/2addr v3, v0

    .line 73
    invoke-virtual {p0}, LX/08Vk;->LJIJI()Z

    .line 74
    .line 75
    .line 76
    move-result v0

    .line 77
    if-eqz v0, :cond_64

    .line 78
    .line 79
    invoke-virtual {p0}, LX/08Vk;->LJIILJJIL()Z

    .line 80
    .line 81
    .line 82
    move-result v0

    .line 83
    if-eqz v0, :cond_65

    .line 84
    .line 85
    const/16 v0, 0x7a

    .line 86
    .line 87
    :goto_57
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 88
    .line 89
    .line 90
    move-result-object v0

    .line 91
    invoke-static {v0}, LX/04J6;->LIZIZ(Ljava/lang/Number;)F

    .line 92
    .line 93
    .line 94
    move-result v0

    .line 95
    invoke-static {v0}, LX/06EZ;->LIZJ(F)I

    .line 96
    .line 97
    .line 98
    move-result v3

    .line 99
    add-int/2addr v3, v2

    .line 100
    :cond_64
    return v3

    .line 101
    :cond_65
    const/16 v0, 0x76

    .line 102
    .line 103
    goto :goto_57

    .line 104
    :cond_68
    const/4 v0, 0x0

    .line 105
    goto :goto_48
.end method

.method public final LJIJJLI()I
    .registers 3

    .prologue
    .line 0
    invoke-virtual {p0}, LX/08Vk;->LJIJI()Z

    .line 1
    .line 2
    .line 3
    move-result v0

    .line 4
    if-eqz v0, :cond_2a

    .line 5
    .line 6
    invoke-virtual {p0}, LX/08Vk;->LJIILJJIL()Z

    .line 7
    .line 8
    .line 9
    move-result v0

    .line 10
    if-eqz v0, :cond_1b

    .line 11
    .line 12
    const/16 v0, 0x30

    .line 13
    .line 14
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 15
    .line 16
    .line 17
    move-result-object v0

    .line 18
    invoke-static {v0}, LX/04J6;->LIZIZ(Ljava/lang/Number;)F

    .line 19
    .line 20
    .line 21
    move-result v0

    .line 22
    invoke-static {v0}, LX/06EZ;->LIZJ(F)I

    .line 23
    .line 24
    .line 25
    move-result v0

    .line 26
    return v0

    .line 27
    :cond_1b
    const/16 v0, 0x50

    .line 28
    .line 29
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 30
    .line 31
    .line 32
    move-result-object v0

    .line 33
    invoke-static {v0}, LX/04J6;->LIZIZ(Ljava/lang/Number;)F

    .line 34
    .line 35
    .line 36
    move-result v0

    .line 37
    invoke-static {v0}, LX/06EZ;->LIZJ(F)I

    .line 38
    .line 39
    .line 40
    move-result v0

    .line 41
    return v0

    .line 42
    :cond_2a
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 43
    .line 44
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 45
    .line 46
    .line 47
    move-result-object v1

    .line 48
    const/high16 v0, 0x42700000    # 60.0f

    .line 49
    .line 50
    invoke-static {v0, v1}, LX/06Tr;->LIZJ(FLandroid/content/Context;)F

    .line 51
    .line 52
    .line 53
    move-result v0

    .line 54
    float-to-int v0, v0

    .line 55
    return v0
.end method

.method public final LJIL(ZZLcom/ss/android/ugc/aweme/feed/model/Aweme;FLjava/lang/String;Ljava/lang/Boolean;)V
    .registers 15

    .prologue
    .line 100663296
    iput-boolean p1, p0, LX/08Vk;->LLLILZLLLI:Z

    .line 100663297
    .line 100663298
    if-nez p2, :cond_5

    .line 100663299
    .line 100663300
    return-void

    .line 100663301
    :cond_5
    invoke-static {}, LX/0AFa;->LIZIZ()Z

    .line 100663302
    .line 100663303
    .line 100663304
    move-result v0

    .line 100663305
    const/4 v4, 0x0

    .line 100663306
    if-eqz v0, :cond_32

    .line 100663307
    .line 100663308
    iget-boolean v0, p0, LX/08Vk;->LLLLIL:Z

    .line 100663309
    .line 100663310
    if-eqz v0, :cond_18

    .line 100663311
    .line 100663312
    invoke-virtual {p0, p3}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 100663313
    .line 100663314
    .line 100663315
    move-result v0

    .line 100663316
    iput-boolean v0, p0, LX/08Vk;->LLLLIILLL:Z

    .line 100663317
    .line 100663318
    iput-boolean v4, p0, LX/08Vk;->LLLLIL:Z

    .line 100663319
    .line 100663320
    :cond_18
    iget-boolean v1, p0, LX/08Vk;->LLLLIILLL:Z

    .line 100663321
    .line 100663322
    :goto_1a
    sget-object v0, LX/06OD;->LIZIZ:LX/06OD;

    .line 100663323
    .line 100663324
    invoke-virtual {v0, p3}, LX/06OD;->LJJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 100663325
    .line 100663326
    .line 100663327
    move-result v0

    .line 100663328
    if-eqz v0, :cond_37

    .line 100663329
    .line 100663330
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663331
    .line 100663332
    invoke-virtual {v0}, Landroid/view/View;->getVisibility()I

    .line 100663333
    .line 100663334
    .line 100663335
    move-result v0

    .line 100663336
    const/16 v1, 0x8

    .line 100663337
    .line 100663338
    if-eq v0, v1, :cond_31

    .line 100663339
    .line 100663340
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663341
    .line 100663342
    invoke-static {v0, v1}, LX/00kD;->e0(LX/08Ic;I)V

    .line 100663343
    .line 100663344
    .line 100663345
    :cond_31
    return-void

    .line 100663346
    :cond_32
    invoke-virtual {p0, p3}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 100663347
    .line 100663348
    .line 100663349
    move-result v1

    .line 100663350
    goto :goto_1a

    .line 100663351
    :cond_37
    const/4 v5, 0x1

    .line 100663352
    if-nez v1, :cond_44

    .line 100663353
    .line 100663354
    if-eqz p3, :cond_d8

    .line 100663355
    .line 100663356
    invoke-virtual {p3}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAwemeType()I

    .line 100663357
    .line 100663358
    .line 100663359
    move-result v1

    .line 100663360
    const/16 v0, 0x1f6

    .line 100663361
    .line 100663362
    if-ne v1, v0, :cond_d8

    .line 100663363
    .line 100663364
    :cond_44
    iget-boolean v0, p0, LX/08Vk;->LLLF:Z

    .line 100663365
    .line 100663366
    if-nez v0, :cond_d8

    .line 100663367
    .line 100663368
    iget-object v0, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 100663369
    .line 100663370
    if-nez v0, :cond_57

    .line 100663371
    .line 100663372
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 100663373
    .line 100663374
    invoke-static {v0, p3}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 100663375
    .line 100663376
    .line 100663377
    move-result v0

    .line 100663378
    if-eqz v0, :cond_57

    .line 100663379
    .line 100663380
    invoke-virtual {p0, p3}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 100663381
    .line 100663382
    .line 100663383
    :cond_57
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 100663384
    .line 100663385
    .line 100663386
    move-result v0

    .line 100663387
    if-eqz v0, :cond_81

    .line 100663388
    .line 100663389
    sget-object v0, LX/03eb;->LIZIZ:LX/03eb;

    .line 100663390
    .line 100663391
    invoke-virtual {v0}, LX/03eb;->LJ()LX/03ef;

    .line 100663392
    .line 100663393
    .line 100663394
    move-result-object v0

    .line 100663395
    invoke-interface {v0}, LX/03ef;->LJIJJ()LX/03jy;

    .line 100663396
    .line 100663397
    .line 100663398
    move-result-object v0

    .line 100663399
    invoke-interface {v0}, LX/03jy;->LLJLL()Z

    .line 100663400
    .line 100663401
    .line 100663402
    move-result v0

    .line 100663403
    if-nez v0, :cond_81

    .line 100663404
    .line 100663405
    iget v0, p0, LX/08Vk;->LLLILZJ:I

    .line 100663406
    .line 100663407
    add-int/lit8 v1, v0, 0x1

    .line 100663408
    .line 100663409
    iput v1, p0, LX/08Vk;->LLLILZJ:I

    .line 100663410
    .line 100663411
    const/4 v0, 0x3

    .line 100663412
    if-le v1, v0, :cond_81

    .line 100663413
    .line 100663414
    iput v4, p0, LX/08Vk;->LLLILZJ:I

    .line 100663415
    .line 100663416
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663417
    .line 100663418
    invoke-virtual {p0, v4}, LX/08Vk;->LJIIJJI(Z)I

    .line 100663419
    .line 100663420
    .line 100663421
    move-result v0

    .line 100663422
    invoke-virtual {v1, v0}, LX/08Ic;->setSeekBarShowType(I)V

    .line 100663423
    .line 100663424
    .line 100663425
    :cond_81
    invoke-static {}, LX/0AFa;->LIZIZ()Z

    .line 100663426
    .line 100663427
    .line 100663428
    move-result v0

    .line 100663429
    if-eqz v0, :cond_8b

    .line 100663430
    .line 100663431
    iget-boolean v0, p0, LX/08Vk;->LLLLII:Z

    .line 100663432
    .line 100663433
    if-nez v0, :cond_a6

    .line 100663434
    .line 100663435
    :cond_8b
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663436
    .line 100663437
    invoke-virtual {v0}, Landroid/view/View;->getVisibility()I

    .line 100663438
    .line 100663439
    .line 100663440
    move-result v0

    .line 100663441
    if-eqz v0, :cond_a6

    .line 100663442
    .line 100663443
    invoke-static {}, LX/0AFa;->LIZIZ()Z

    .line 100663444
    .line 100663445
    .line 100663446
    move-result v0

    .line 100663447
    if-eqz v0, :cond_a1

    .line 100663448
    .line 100663449
    sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    .line 100663450
    .line 100663451
    invoke-static {p6, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 100663452
    .line 100663453
    .line 100663454
    move-result v0

    .line 100663455
    if-eqz v0, :cond_a6

    .line 100663456
    .line 100663457
    :cond_a1
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663458
    .line 100663459
    invoke-static {v0, v4}, LX/00kD;->e0(LX/08Ic;I)V

    .line 100663460
    .line 100663461
    .line 100663462
    :cond_a6
    iget-wide v0, p0, LX/08Vk;->LLLFZ:J

    .line 100663463
    .line 100663464
    const-wide/16 v6, 0x0

    .line 100663465
    .line 100663466
    cmp-long v2, v0, v6

    .line 100663467
    .line 100663468
    if-eqz v2, :cond_bc

    .line 100663469
    .line 100663470
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J

    .line 100663471
    .line 100663472
    .line 100663473
    move-result-wide v6

    .line 100663474
    iget-wide v2, p0, LX/08Vk;->LLLFZ:J

    .line 100663475
    .line 100663476
    const/16 v0, 0x258

    .line 100663477
    .line 100663478
    int-to-long v0, v0

    .line 100663479
    add-long/2addr v2, v0

    .line 100663480
    cmp-long v0, v6, v2

    .line 100663481
    .line 100663482
    if-lez v0, :cond_d8

    .line 100663483
    .line 100663484
    :cond_bc
    sget-object v0, LX/0CQ2;->LIZ:LX/0CQ2;

    .line 100663485
    .line 100663486
    if-eqz p3, :cond_d9

    .line 100663487
    .line 100663488
    invoke-virtual {p3}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 100663489
    .line 100663490
    .line 100663491
    move-result-object v1

    .line 100663492
    :goto_c4
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 100663493
    .line 100663494
    .line 100663495
    const-string/jumbo v0, "seekBar_setProgress"

    .line 100663496
    .line 100663497
    .line 100663498
    invoke-static {p4, v1, p5, v0}, LX/0CQ2;->LIZ(FLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    .line 100663499
    .line 100663500
    .line 100663501
    invoke-static {}, LX/0BHV;->LIZ()Z

    .line 100663502
    .line 100663503
    .line 100663504
    move-result v0

    .line 100663505
    if-eqz v0, :cond_db

    .line 100663506
    .line 100663507
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663508
    .line 100663509
    invoke-virtual {v0, p4, v5}, LX/08Ic;->LIZIZ(FZ)V

    .line 100663510
    .line 100663511
    .line 100663512
    :cond_d8
    return-void

    .line 100663513
    :cond_d9
    const/4 v1, 0x0

    .line 100663514
    goto :goto_c4

    .line 100663515
    :cond_db
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 100663516
    .line 100663517
    sget v0, LX/08Ic;->LLL:F

    .line 100663518
    .line 100663519
    invoke-virtual {v1, p4, v4}, LX/08Ic;->LIZIZ(FZ)V

    .line 100663520
    .line 100663521
    .line 100663522
    return-void
.end method

.method public final LJJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;IZ)V
    .registers 7

    .prologue
    .line 50331648
    invoke-virtual {p0, p1}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 50331649
    .line 50331650
    .line 50331651
    move-result v0

    .line 50331652
    const/4 v2, 0x4

    .line 50331653
    if-eqz v0, :cond_46

    .line 50331654
    .line 50331655
    if-eqz p3, :cond_46

    .line 50331656
    .line 50331657
    if-nez p2, :cond_36

    .line 50331658
    .line 50331659
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 50331660
    .line 50331661
    const/4 v2, 0x0

    .line 50331662
    invoke-static {v0, v2}, LX/00kD;->e0(LX/08Ic;I)V

    .line 50331663
    .line 50331664
    .line 50331665
    invoke-static {}, LX/0AFa;->LIZIZ()Z

    .line 50331666
    .line 50331667
    .line 50331668
    move-result v0

    .line 50331669
    if-eqz v0, :cond_26

    .line 50331670
    .line 50331671
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 50331672
    .line 50331673
    invoke-virtual {v1}, LX/08Ic;->getMProgress()Ljava/lang/Float;

    .line 50331674
    .line 50331675
    .line 50331676
    move-result-object v0

    .line 50331677
    if-eqz v0, :cond_34

    .line 50331678
    .line 50331679
    invoke-virtual {v0}, Ljava/lang/Float;->floatValue()F

    .line 50331680
    .line 50331681
    .line 50331682
    move-result v0

    .line 50331683
    :goto_23
    invoke-virtual {v1, v0}, LX/08Ic;->setProgress(F)V

    .line 50331684
    .line 50331685
    .line 50331686
    :cond_26
    iput-boolean v2, p0, LX/08Vk;->LLLLII:Z

    .line 50331687
    .line 50331688
    iget-boolean v0, p0, LX/08Vk;->LLLF:Z

    .line 50331689
    .line 50331690
    if-eqz v0, :cond_3e

    .line 50331691
    .line 50331692
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 50331693
    .line 50331694
    .line 50331695
    move-result-object v0

    .line 50331696
    invoke-virtual {v0}, LX/08WY;->LIZJ()V

    .line 50331697
    .line 50331698
    .line 50331699
    return-void

    .line 50331700
    :cond_34
    const/4 v0, 0x0

    .line 50331701
    goto :goto_23

    .line 50331702
    :cond_36
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 50331703
    .line 50331704
    invoke-static {v0, v2}, LX/00kD;->e0(LX/08Ic;I)V

    .line 50331705
    .line 50331706
    .line 50331707
    const/4 v0, 0x1

    .line 50331708
    iput-boolean v0, p0, LX/08Vk;->LLLLII:Z

    .line 50331709
    .line 50331710
    :cond_3e
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 50331711
    .line 50331712
    .line 50331713
    move-result-object v0

    .line 50331714
    invoke-virtual {v0}, LX/08WY;->LIZIZ()V

    .line 50331715
    .line 50331716
    .line 50331717
    return-void

    .line 50331718
    :cond_46
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 50331719
    .line 50331720
    const-string/jumbo v0, "updateSeekBarByPageChange can\'t show seekbar"

    .line 50331721
    .line 50331722
    .line 50331723
    invoke-virtual {v1, v0, p1}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 50331724
    .line 50331725
    .line 50331726
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 50331727
    .line 50331728
    invoke-static {v0, v2}, LX/00kD;->e0(LX/08Ic;I)V

    .line 50331729
    .line 50331730
    .line 50331731
    return-void
.end method

.method public final LJJI(Z)V
    .registers 17

    .prologue
    .line 16777216
    iget-boolean v0, p0, LX/08Vk;->LLLIIL:Z

    .line 16777217
    .line 16777218
    const/4 v14, 0x0

    .line 16777219
    const/4 v7, 0x1

    .line 16777220
    if-nez v0, :cond_4e

    .line 16777221
    .line 16777222
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777223
    .line 16777224
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 16777225
    .line 16777226
    .line 16777227
    move-result-object v0

    .line 16777228
    invoke-static {v0}, LX/04IH;->LIZIZ(Landroid/content/Context;)LX/02IC;

    .line 16777229
    .line 16777230
    .line 16777231
    move-result-object v2

    .line 16777232
    if-eqz v2, :cond_4e

    .line 16777233
    .line 16777234
    iput-boolean v7, p0, LX/08Vk;->LLLIIL:Z

    .line 16777235
    .line 16777236
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777237
    .line 16777238
    if-eqz v0, :cond_34

    .line 16777239
    .line 16777240
    invoke-interface {v0}, LX/05fP;->getFragment()Landroidx/fragment/app/Fragment;

    .line 16777241
    .line 16777242
    .line 16777243
    move-result-object v1

    .line 16777244
    if-eqz v1, :cond_34

    .line 16777245
    .line 16777246
    const-class v9, Lcom/ss/android/ugc/aweme/IDetailFeedSafService;

    .line 16777247
    .line 16777248
    const/4 v10, 0x0

    .line 16777249
    const/16 v13, 0xe

    .line 16777250
    .line 16777251
    move v11, v10

    .line 16777252
    move v12, v10

    .line 16777253
    invoke-static/range {v9 .. v14}, Lcom/ss/android/ugc/aweme/framework/services/PluggableExtentionKt;->pluggableSpi$default(Ljava/lang/Class;ZZZILjava/lang/Object;)Ljava/lang/Object;

    .line 16777254
    .line 16777255
    .line 16777256
    move-result-object v0

    .line 16777257
    check-cast v0, Lcom/ss/android/ugc/aweme/IDetailFeedSafService;

    .line 16777258
    .line 16777259
    if-eqz v0, :cond_34

    .line 16777260
    .line 16777261
    invoke-interface {v0, v1}, Lcom/ss/android/ugc/aweme/IDetailFeedSafService;->LIZLLL(Landroidx/fragment/app/Fragment;)Landroidx/fragment/app/Fragment;

    .line 16777262
    .line 16777263
    .line 16777264
    move-result-object v0

    .line 16777265
    if-eqz v0, :cond_34

    .line 16777266
    .line 16777267
    move-object v2, v0

    .line 16777268
    :cond_34
    iget-object v1, p0, LX/08Vk;->LLJJJIL:Lcom/ss/android/ugc/aweme/homepage/api/interaction/ScrollSwitchStateManager;

    .line 16777269
    .line 16777270
    if-eqz v1, :cond_40

    .line 16777271
    .line 16777272
    new-instance v0, LX/0BHS;

    .line 16777273
    .line 16777274
    invoke-direct {v0, p0}, LX/0BHS;-><init>(LX/08Vk;)V

    .line 16777275
    .line 16777276
    .line 16777277
    invoke-virtual {v1, v2, v0}, Lcom/ss/android/ugc/aweme/homepage/api/interaction/BaseScrollSwitchStateManager;->m53(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    .line 16777278
    .line 16777279
    .line 16777280
    :cond_40
    iget-object v0, p0, LX/08Vk;->LLJJJIL:Lcom/ss/android/ugc/aweme/homepage/api/interaction/ScrollSwitchStateManager;

    .line 16777281
    .line 16777282
    if-eqz v0, :cond_4e

    .line 16777283
    .line 16777284
    new-instance v1, LX/0BHT;

    .line 16777285
    .line 16777286
    invoke-direct {v1, p0}, LX/0BHT;-><init>(LX/08Vk;)V

    .line 16777287
    .line 16777288
    .line 16777289
    iget-object v0, v0, Lcom/ss/android/ugc/aweme/homepage/api/interaction/BaseScrollSwitchStateManager;->LLLI:Landroidx/lifecycle/MutableLiveData;

    .line 16777290
    .line 16777291
    invoke-virtual {v0, v2, v1}, Landroidx/lifecycle/LiveData;->observe(Landroidx/lifecycle/LifecycleOwner;Landroidx/lifecycle/Observer;)V

    .line 16777292
    .line 16777293
    .line 16777294
    :cond_4e
    iget-object v3, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777295
    .line 16777296
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777297
    .line 16777298
    invoke-static {v0}, LX/0BHU;->LIZJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/Float;

    .line 16777299
    .line 16777300
    .line 16777301
    move-result-object v2

    .line 16777302
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777303
    .line 16777304
    invoke-static {v0}, LX/0BHU;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/Float;

    .line 16777305
    .line 16777306
    .line 16777307
    move-result-object v1

    .line 16777308
    iget-object v0, v3, LX/08Ic;->LLJJIJIIJIL:LX/08Io;

    .line 16777309
    .line 16777310
    if-eqz v0, :cond_69

    .line 16777311
    .line 16777312
    iget-object v0, v0, LX/08Io;->LLJJJJ:LX/08T4;

    .line 16777313
    .line 16777314
    iput-object v2, v0, LX/08T4;->LJIIIZ:Ljava/lang/Float;

    .line 16777315
    .line 16777316
    iput-object v1, v0, LX/08T4;->LJIIJ:Ljava/lang/Float;

    .line 16777317
    .line 16777318
    invoke-virtual {v0}, Landroid/graphics/drawable/Drawable;->invalidateSelf()V

    .line 16777319
    .line 16777320
    .line 16777321
    :cond_69
    iget-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777322
    .line 16777323
    const-string v10, "paid_series_detail_page"

    .line 16777324
    .line 16777325
    invoke-static {v0, v10}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777326
    .line 16777327
    .line 16777328
    move-result v0

    .line 16777329
    xor-int/lit8 v0, v0, 0x1

    .line 16777330
    .line 16777331
    const/4 v9, 0x0

    .line 16777332
    if-eqz v0, :cond_7d

    .line 16777333
    .line 16777334
    if-eqz p1, :cond_7d

    .line 16777335
    .line 16777336
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777337
    .line 16777338
    invoke-virtual {v0, v9}, LX/08Ic;->setProgress(F)V

    .line 16777339
    .line 16777340
    .line 16777341
    :cond_7d
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777342
    .line 16777343
    invoke-virtual {p0, v0}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777344
    .line 16777345
    .line 16777346
    move-result v0

    .line 16777347
    const/4 v8, 0x0

    .line 16777348
    const/16 v6, 0x8

    .line 16777349
    .line 16777350
    if-nez v0, :cond_94

    .line 16777351
    .line 16777352
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777353
    .line 16777354
    if-eqz v0, :cond_26d

    .line 16777355
    .line 16777356
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAwemeType()I

    .line 16777357
    .line 16777358
    .line 16777359
    move-result v1

    .line 16777360
    const/16 v0, 0x1f6

    .line 16777361
    .line 16777362
    if-ne v1, v0, :cond_26d

    .line 16777363
    .line 16777364
    :cond_94
    iget-object v0, p0, LX/08Vk;->LLJLLL:Ljava/util/List;

    .line 16777365
    .line 16777366
    check-cast v0, Ljava/util/ArrayList;

    .line 16777367
    .line 16777368
    invoke-virtual {v0}, Ljava/util/ArrayList;->clear()V

    .line 16777369
    .line 16777370
    .line 16777371
    new-instance v5, Ljava/util/ArrayList;

    .line 16777372
    .line 16777373
    invoke-direct {v5}, Ljava/util/ArrayList;-><init>()V

    .line 16777374
    .line 16777375
    .line 16777376
    invoke-virtual {p0}, LX/08Vk;->LJIILL()Z

    .line 16777377
    .line 16777378
    .line 16777379
    move-result v0

    .line 16777380
    if-eqz v0, :cond_11d

    .line 16777381
    .line 16777382
    iget-object v3, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777383
    .line 16777384
    if-eqz v3, :cond_f6

    .line 16777385
    .line 16777386
    invoke-static {v3}, LX/0Bzu;->LIZJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Lcom/ss/android/ugc/aweme/search/source/neo/ISearchContextAbility;

    .line 16777387
    .line 16777388
    .line 16777389
    move-result-object v0

    .line 16777390
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/search/source/neo/ISearchContextAbility;->L80()LX/0C2F;

    .line 16777391
    .line 16777392
    .line 16777393
    move-result-object v0

    .line 16777394
    invoke-virtual {v0}, LX/0C2F;->LIZJ()LX/0C1D;

    .line 16777395
    .line 16777396
    .line 16777397
    move-result-object v0

    .line 16777398
    check-cast v0, LX/0C2k;

    .line 16777399
    .line 16777400
    iget-object v0, v0, LX/0C2k;->LLJJJJ:Ljava/util/List;

    .line 16777401
    .line 16777402
    invoke-interface {v0}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;

    .line 16777403
    .line 16777404
    .line 16777405
    move-result-object v13

    .line 16777406
    :cond_be
    :goto_be
    invoke-interface {v13}, Ljava/util/Iterator;->hasNext()Z

    .line 16777407
    .line 16777408
    .line 16777409
    move-result v0

    .line 16777410
    if-eqz v0, :cond_f6

    .line 16777411
    .line 16777412
    invoke-interface {v13}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 16777413
    .line 16777414
    .line 16777415
    move-result-object v2

    .line 16777416
    check-cast v2, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;

    .line 16777417
    .line 16777418
    invoke-virtual {v3}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777419
    .line 16777420
    .line 16777421
    move-result-object v0

    .line 16777422
    if-eqz v0, :cond_f4

    .line 16777423
    .line 16777424
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777425
    .line 16777426
    .line 16777427
    move-result v12

    .line 16777428
    :goto_d4
    invoke-static {v12}, LX/0BX3;->LIZJ(I)I

    .line 16777429
    .line 16777430
    .line 16777431
    move-result v4

    .line 16777432
    iget v1, v2, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;->secondStart:I

    .line 16777433
    .line 16777434
    int-to-float v11, v1

    .line 16777435
    const/high16 v0, 0x447a0000    # 1000.0f

    .line 16777436
    .line 16777437
    mul-float/2addr v11, v0

    .line 16777438
    int-to-float v0, v12

    .line 16777439
    cmpg-float v0, v11, v0

    .line 16777440
    .line 16777441
    if-gez v0, :cond_be

    .line 16777442
    .line 16777443
    invoke-static {v1}, LX/08Vk;->LJIIIIZZ(I)I

    .line 16777444
    .line 16777445
    .line 16777446
    move-result v0

    .line 16777447
    int-to-float v1, v0

    .line 16777448
    int-to-float v0, v4

    .line 16777449
    div-float/2addr v1, v0

    .line 16777450
    iput v1, v2, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;->LIZIZ:F

    .line 16777451
    .line 16777452
    iget-object v0, p0, LX/08Vk;->LLJLLL:Ljava/util/List;

    .line 16777453
    .line 16777454
    check-cast v0, Ljava/util/ArrayList;

    .line 16777455
    .line 16777456
    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 16777457
    .line 16777458
    .line 16777459
    goto :goto_be

    .line 16777460
    :cond_f4
    const/4 v12, 0x0

    .line 16777461
    goto :goto_d4

    .line 16777462
    :cond_f6
    iget-object v1, p0, LX/08Vk;->LLJLLL:Ljava/util/List;

    .line 16777463
    .line 16777464
    new-instance v2, Ljava/util/ArrayList;

    .line 16777465
    .line 16777466
    const/16 v0, 0xa

    .line 16777467
    .line 16777468
    invoke-static {v1, v0}, LX/036N;->LJIJI(Ljava/lang/Iterable;I)I

    .line 16777469
    .line 16777470
    .line 16777471
    move-result v0

    .line 16777472
    invoke-direct {v2, v0}, Ljava/util/ArrayList;-><init>(I)V

    .line 16777473
    .line 16777474
    .line 16777475
    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 16777476
    .line 16777477
    .line 16777478
    move-result-object v1

    .line 16777479
    :goto_107
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 16777480
    .line 16777481
    .line 16777482
    move-result v0

    .line 16777483
    if-eqz v0, :cond_18c

    .line 16777484
    .line 16777485
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 16777486
    .line 16777487
    .line 16777488
    move-result-object v0

    .line 16777489
    check-cast v0, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;

    .line 16777490
    .line 16777491
    iget v0, v0, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;->LIZIZ:F

    .line 16777492
    .line 16777493
    invoke-static {v0}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    .line 16777494
    .line 16777495
    .line 16777496
    move-result-object v0

    .line 16777497
    invoke-virtual {v2, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 16777498
    .line 16777499
    .line 16777500
    goto :goto_107

    .line 16777501
    :cond_11d
    invoke-static {}, LX/0BWw;->LIZ()Z

    .line 16777502
    .line 16777503
    .line 16777504
    move-result v0

    .line 16777505
    if-eqz v0, :cond_18f

    .line 16777506
    .line 16777507
    sget-object v1, LX/04Nf;->LIZIZ:LX/04Nf;

    .line 16777508
    .line 16777509
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777510
    .line 16777511
    if-eqz v0, :cond_18a

    .line 16777512
    .line 16777513
    invoke-interface {v0, v7}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 16777514
    .line 16777515
    .line 16777516
    move-result-object v0

    .line 16777517
    :goto_12d
    invoke-virtual {v1, v0}, LX/04Nf;->LJJJJL(Ljava/lang/String;)Z

    .line 16777518
    .line 16777519
    .line 16777520
    move-result v0

    .line 16777521
    if-eqz v0, :cond_18f

    .line 16777522
    .line 16777523
    iget-object v2, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777524
    .line 16777525
    if-eqz v2, :cond_18f

    .line 16777526
    .line 16777527
    invoke-virtual {v2}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777528
    .line 16777529
    .line 16777530
    move-result-object v0

    .line 16777531
    if-eqz v0, :cond_188

    .line 16777532
    .line 16777533
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777534
    .line 16777535
    .line 16777536
    move-result v4

    .line 16777537
    :goto_141
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777538
    .line 16777539
    invoke-virtual {v0}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 16777540
    .line 16777541
    .line 16777542
    move-result-object v0

    .line 16777543
    invoke-static {v0}, LX/04IH;->LIZIZ(Landroid/content/Context;)LX/02IC;

    .line 16777544
    .line 16777545
    .line 16777546
    move-result-object v1

    .line 16777547
    if-eqz v1, :cond_18f

    .line 16777548
    .line 16777549
    sget-object v0, LX/04Nf;->LIZIZ:LX/04Nf;

    .line 16777550
    .line 16777551
    invoke-virtual {v0, v1}, LX/04Nf;->LLF(LX/02IC;)Ljava/util/Map;

    .line 16777552
    .line 16777553
    .line 16777554
    move-result-object v1

    .line 16777555
    if-eqz v1, :cond_18f

    .line 16777556
    .line 16777557
    invoke-virtual {v2}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777558
    .line 16777559
    .line 16777560
    move-result-object v0

    .line 16777561
    invoke-interface {v1, v0}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 16777562
    .line 16777563
    .line 16777564
    move-result-object v0

    .line 16777565
    check-cast v0, Ljava/util/List;

    .line 16777566
    .line 16777567
    if-eqz v0, :cond_18f

    .line 16777568
    .line 16777569
    invoke-interface {v0}, Ljava/lang/Iterable;->iterator()Ljava/util/Iterator;

    .line 16777570
    .line 16777571
    .line 16777572
    move-result-object v12

    .line 16777573
    :goto_165
    invoke-interface {v12}, Ljava/util/Iterator;->hasNext()Z

    .line 16777574
    .line 16777575
    .line 16777576
    move-result v0

    .line 16777577
    if-eqz v0, :cond_18f

    .line 16777578
    .line 16777579
    invoke-interface {v12}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 16777580
    .line 16777581
    .line 16777582
    move-result-object v0

    .line 16777583
    check-cast v0, Lcom/ss/android/ugc/aweme/tako/detail/model/TakoKeyFramePoint;

    .line 16777584
    .line 16777585
    iget-wide v2, v0, Lcom/ss/android/ugc/aweme/tako/detail/model/TakoKeyFramePoint;->timestamp:D

    .line 16777586
    .line 16777587
    const/16 v0, 0x3e8

    .line 16777588
    .line 16777589
    int-to-double v0, v0

    .line 16777590
    mul-double/2addr v2, v0

    .line 16777591
    int-to-double v0, v4

    .line 16777592
    cmpg-double v11, v2, v0

    .line 16777593
    .line 16777594
    if-gez v11, :cond_186

    .line 16777595
    .line 16777596
    div-double/2addr v2, v0

    .line 16777597
    double-to-float v0, v2

    .line 16777598
    :goto_17e
    invoke-static {v0}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    .line 16777599
    .line 16777600
    .line 16777601
    move-result-object v0

    .line 16777602
    invoke-virtual {v5, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 16777603
    .line 16777604
    .line 16777605
    goto :goto_165

    .line 16777606
    :cond_186
    const/4 v0, 0x0

    .line 16777607
    goto :goto_17e

    .line 16777608
    :cond_188
    const/4 v4, 0x0

    .line 16777609
    goto :goto_141

    .line 16777610
    :cond_18a
    move-object v0, v14

    .line 16777611
    goto :goto_12d

    .line 16777612
    :cond_18c
    invoke-virtual {v5, v2}, Ljava/util/ArrayList;->addAll(Ljava/util/Collection;)Z

    .line 16777613
    .line 16777614
    .line 16777615
    :cond_18f
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777616
    .line 16777617
    invoke-virtual {v0, v5}, LX/08Ic;->setKeyPointsProgressPositions(Ljava/util/List;)V

    .line 16777618
    .line 16777619
    .line 16777620
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777621
    .line 16777622
    invoke-static {v1}, LX/06w5;->LIZJ(Ljava/lang/Object;)V

    .line 16777623
    .line 16777624
    .line 16777625
    const/high16 v0, 0x3f800000    # 1.0f

    .line 16777626
    .line 16777627
    invoke-virtual {v1, v0}, Landroid/view/View;->setAlpha(F)V

    .line 16777628
    .line 16777629
    .line 16777630
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777631
    .line 16777632
    invoke-static {v0, v8}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777633
    .line 16777634
    .line 16777635
    iget-object v2, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777636
    .line 16777637
    invoke-static {}, LX/02nu;->LIZ()Ljava/lang/StringBuilder;

    .line 16777638
    .line 16777639
    .line 16777640
    move-result-object v1

    .line 16777641
    const-string/jumbo v0, "show progressbar:"

    .line 16777642
    .line 16777643
    .line 16777644
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777645
    .line 16777646
    .line 16777647
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777648
    .line 16777649
    if-eqz v0, :cond_26a

    .line 16777650
    .line 16777651
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777652
    .line 16777653
    .line 16777654
    move-result-object v0

    .line 16777655
    :goto_1b7
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777656
    .line 16777657
    .line 16777658
    invoke-static {v1}, LX/02nu;->LIZIZ(Ljava/lang/StringBuilder;)Ljava/lang/String;

    .line 16777659
    .line 16777660
    .line 16777661
    move-result-object v0

    .line 16777662
    invoke-virtual {v2, v0}, LX/05f6;->LJ(Ljava/lang/String;)V

    .line 16777663
    .line 16777664
    .line 16777665
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 16777666
    .line 16777667
    .line 16777668
    move-result-object v0

    .line 16777669
    invoke-virtual {v0}, LX/08WY;->LIZIZ()V

    .line 16777670
    .line 16777671
    .line 16777672
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777673
    .line 16777674
    if-eqz v0, :cond_267

    .line 16777675
    .line 16777676
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777677
    .line 16777678
    .line 16777679
    move-result-object v0

    .line 16777680
    if-eqz v0, :cond_267

    .line 16777681
    .line 16777682
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777683
    .line 16777684
    .line 16777685
    move-result v0

    .line 16777686
    :goto_1d6
    invoke-static {v0}, LX/0BX3;->LIZJ(I)I

    .line 16777687
    .line 16777688
    .line 16777689
    move-result v0

    .line 16777690
    iput v0, p0, LX/08Vk;->LLL:I

    .line 16777691
    .line 16777692
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777693
    .line 16777694
    if-eqz v0, :cond_1e3

    .line 16777695
    .line 16777696
    invoke-static {v0}, LX/0AGg;->LJIIIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Lcom/ss/android/ugc/aweme/feed/model/CaptionItemModel;

    .line 16777697
    .line 16777698
    .line 16777699
    :cond_1e3
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 16777700
    .line 16777701
    .line 16777702
    move-result-object v0

    .line 16777703
    invoke-virtual {v0}, LX/08WY;->LIZ()V

    .line 16777704
    .line 16777705
    .line 16777706
    invoke-static {}, LX/0BHV;->LIZ()Z

    .line 16777707
    .line 16777708
    .line 16777709
    move-result v0

    .line 16777710
    if-eqz v0, :cond_20d

    .line 16777711
    .line 16777712
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777713
    .line 16777714
    invoke-virtual {v0}, LX/08Ic;->getMutableSeekBar()LX/08Io;

    .line 16777715
    .line 16777716
    .line 16777717
    move-result-object v1

    .line 16777718
    if-eqz v1, :cond_20d

    .line 16777719
    .line 16777720
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777721
    .line 16777722
    if-eqz v0, :cond_262

    .line 16777723
    .line 16777724
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777725
    .line 16777726
    .line 16777727
    move-result-object v0

    .line 16777728
    if-eqz v0, :cond_262

    .line 16777729
    .line 16777730
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777731
    .line 16777732
    .line 16777733
    move-result v0

    .line 16777734
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 16777735
    .line 16777736
    .line 16777737
    move-result-object v0

    .line 16777738
    :goto_20a
    invoke-virtual {v1, v0}, LX/08Io;->setVideoDuration(Ljava/lang/Integer;)V

    .line 16777739
    .line 16777740
    .line 16777741
    :cond_20d
    iget-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777742
    .line 16777743
    invoke-static {v0, v10}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777744
    .line 16777745
    .line 16777746
    move-result v0

    .line 16777747
    xor-int/lit8 v0, v0, 0x1

    .line 16777748
    .line 16777749
    if-eqz v0, :cond_21e

    .line 16777750
    .line 16777751
    if-eqz p1, :cond_21e

    .line 16777752
    .line 16777753
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777754
    .line 16777755
    invoke-virtual {v0, v9, v8}, LX/08Ic;->LIZIZ(FZ)V

    .line 16777756
    .line 16777757
    .line 16777758
    :cond_21e
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777759
    .line 16777760
    iget-boolean v0, p0, LX/08Vk;->LLLILZLLLI:Z

    .line 16777761
    .line 16777762
    if-eqz v0, :cond_25d

    .line 16777763
    .line 16777764
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 16777765
    .line 16777766
    .line 16777767
    move-result v0

    .line 16777768
    if-eqz v0, :cond_25d

    .line 16777769
    .line 16777770
    const/4 v0, 0x1

    .line 16777771
    :goto_22b
    invoke-virtual {v1, v0}, LX/08Ic;->setSeekBarShowType(I)V

    .line 16777772
    .line 16777773
    .line 16777774
    sget-object v0, LX/0BX9;->LIZ:LX/0BX9;

    .line 16777775
    .line 16777776
    iget-object v1, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777777
    .line 16777778
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777779
    .line 16777780
    .line 16777781
    invoke-static {v1}, LX/0B7V;->LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777782
    .line 16777783
    .line 16777784
    move-result v0

    .line 16777785
    if-eqz v0, :cond_252

    .line 16777786
    .line 16777787
    sget-object v0, LX/0AC1;->LIZLLL:LX/02pj;

    .line 16777788
    .line 16777789
    invoke-static {}, LX/0AC3;->LIZ()LX/0AC1;

    .line 16777790
    .line 16777791
    .line 16777792
    move-result-object v0

    .line 16777793
    if-eqz v1, :cond_247

    .line 16777794
    .line 16777795
    invoke-virtual {v1}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777796
    .line 16777797
    .line 16777798
    move-result-object v14

    .line 16777799
    :cond_247
    invoke-virtual {v0, v14}, LX/0AC1;->LIZ(Ljava/lang/String;)Z

    .line 16777800
    .line 16777801
    .line 16777802
    move-result v0

    .line 16777803
    if-eqz v0, :cond_252

    .line 16777804
    .line 16777805
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777806
    .line 16777807
    invoke-static {v0, v6}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777808
    .line 16777809
    .line 16777810
    :cond_252
    invoke-static {}, LX/0AFa;->LIZIZ()Z

    .line 16777811
    .line 16777812
    .line 16777813
    move-result v0

    .line 16777814
    if-eqz v0, :cond_25c

    .line 16777815
    .line 16777816
    iput-boolean v7, p0, LX/08Vk;->LLLLIL:Z

    .line 16777817
    .line 16777818
    iput-boolean v8, p0, LX/08Vk;->LLLLIILLL:Z

    .line 16777819
    .line 16777820
    :cond_25c
    return-void

    .line 16777821
    :cond_25d
    invoke-virtual {p0, v8}, LX/08Vk;->LJIIJJI(Z)I

    .line 16777822
    .line 16777823
    .line 16777824
    move-result v0

    .line 16777825
    goto :goto_22b

    .line 16777826
    :cond_262
    invoke-static {v8}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 16777827
    .line 16777828
    .line 16777829
    move-result-object v0

    .line 16777830
    goto :goto_20a

    .line 16777831
    :cond_267
    const/4 v0, 0x0

    .line 16777832
    goto/16 :goto_1d6

    .line 16777833
    .line 16777834
    :cond_26a
    move-object v0, v14

    .line 16777835
    goto/16 :goto_1b7

    .line 16777836
    .line 16777837
    :cond_26d
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777838
    .line 16777839
    invoke-static {v0, v6}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777840
    .line 16777841
    .line 16777842
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 16777843
    .line 16777844
    .line 16777845
    move-result-object v0

    .line 16777846
    invoke-virtual {v0}, LX/08WY;->LIZIZ()V

    .line 16777847
    .line 16777848
    .line 16777849
    iget-object v2, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777850
    .line 16777851
    invoke-static {}, LX/02nu;->LIZ()Ljava/lang/StringBuilder;

    .line 16777852
    .line 16777853
    .line 16777854
    move-result-object v1

    .line 16777855
    const-string v0, "hide progressbar:"

    .line 16777856
    .line 16777857
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777858
    .line 16777859
    .line 16777860
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777861
    .line 16777862
    if-eqz v0, :cond_28c

    .line 16777863
    .line 16777864
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777865
    .line 16777866
    .line 16777867
    move-result-object v14

    .line 16777868
    :cond_28c
    invoke-virtual {v1, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777869
    .line 16777870
    .line 16777871
    const-string v0, ", isReverseEnable: "

    .line 16777872
    .line 16777873
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777874
    .line 16777875
    .line 16777876
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777877
    .line 16777878
    invoke-static {v0}, LX/0BHi;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777879
    .line 16777880
    .line 16777881
    move-result v0

    .line 16777882
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    .line 16777883
    .line 16777884
    .line 16777885
    invoke-static {v1}, LX/02nu;->LIZIZ(Ljava/lang/StringBuilder;)Ljava/lang/String;

    .line 16777886
    .line 16777887
    .line 16777888
    move-result-object v0

    .line 16777889
    invoke-virtual {v2, v0}, LX/05f6;->LJ(Ljava/lang/String;)V

    .line 16777890
    .line 16777891
    .line 16777892
    return-void
.end method

.method public final getEventType()Ljava/lang/String;
    .registers 2

    .prologue
    .line 0
    iget-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 1
    .line 2
    return-object v0
.end method

.method public final onBackgroundSurveyEvent(LX/08Wa;)V
    .registers 4
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    iget-object v1, p1, LX/08Wa;->LIZIZ:Ljava/lang/String;

    .line 16777217
    .line 16777218
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777219
    .line 16777220
    if-eqz v0, :cond_40

    .line 16777221
    .line 16777222
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777223
    .line 16777224
    .line 16777225
    move-result-object v0

    .line 16777226
    :goto_a
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777227
    .line 16777228
    .line 16777229
    move-result v0

    .line 16777230
    if-eqz v0, :cond_2c

    .line 16777231
    .line 16777232
    iget-boolean v0, p1, LX/08Wa;->LIZ:Z

    .line 16777233
    .line 16777234
    if-nez v0, :cond_3c

    .line 16777235
    .line 16777236
    invoke-virtual {p0}, LX/08Vk;->LIZJ()V

    .line 16777237
    .line 16777238
    .line 16777239
    :goto_17
    iget-boolean v0, p1, LX/08Wa;->LIZ:Z

    .line 16777240
    .line 16777241
    if-eqz v0, :cond_2d

    .line 16777242
    .line 16777243
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777244
    .line 16777245
    new-instance v0, LX/0iBD;

    .line 16777246
    .line 16777247
    invoke-direct {v0, p1}, LX/0iBD;-><init>(LX/08Wa;)V

    .line 16777248
    .line 16777249
    .line 16777250
    invoke-virtual {v1, v0}, LX/05f6;->LJIIL(Lkotlin/jvm/functions/Function0;)V

    .line 16777251
    .line 16777252
    .line 16777253
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777254
    .line 16777255
    const/16 v0, 0x8

    .line 16777256
    .line 16777257
    invoke-static {v1, v0}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777258
    .line 16777259
    .line 16777260
    :cond_2c
    return-void

    .line 16777261
    :cond_2d
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777262
    .line 16777263
    invoke-virtual {p0, v0}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777264
    .line 16777265
    .line 16777266
    move-result v0

    .line 16777267
    if-eqz v0, :cond_2c

    .line 16777268
    .line 16777269
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777270
    .line 16777271
    const/4 v0, 0x0

    .line 16777272
    invoke-static {v1, v0}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777273
    .line 16777274
    .line 16777275
    return-void

    .line 16777276
    :cond_3c
    invoke-virtual {p0}, LX/08Vk;->LJ()V

    .line 16777277
    .line 16777278
    .line 16777279
    goto :goto_17

    .line 16777280
    :cond_40
    const/4 v0, 0x0

    .line 16777281
    goto :goto_a
.end method

.method public onDestroy()V
    .registers 3

    .prologue
    .line 0
    invoke-static {}, Lorg/greenrobot/eventbus/EventBus;->LIZLLL()Lorg/greenrobot/eventbus/EventBus;

    .line 1
    .line 2
    .line 3
    move-result-object v0

    .line 4
    invoke-virtual {v0, p0}, Lorg/greenrobot/eventbus/EventBus;->LJJI(Ljava/lang/Object;)V

    .line 5
    .line 6
    .line 7
    const/4 v0, 0x0

    .line 8
    iput-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 9
    .line 10
    iget-object v1, p0, LX/08Vk;->LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 11
    .line 12
    iget-object v0, p0, LX/08Vk;->LLLLILI:LX/08Xl;

    .line 13
    .line 14
    invoke-virtual {v1, v0}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 15
    .line 16
    .line 17
    iget-object v0, p0, LX/08Vk;->LLLLIIIILLL:LX/0Lxl;

    .line 18
    .line 19
    if-eqz v0, :cond_18

    .line 20
    .line 21
    invoke-virtual {v0}, LX/0Lxl;->LIZIZ()V

    .line 22
    .line 23
    .line 24
    :cond_18
    return-void
.end method

.method public final onDowngradeRecovery(LX/08Wb;)V
    .registers 7
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    invoke-virtual {p0, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777217
    .line 16777218
    .line 16777219
    move-result v0

    .line 16777220
    if-eqz v0, :cond_52

    .line 16777221
    .line 16777222
    iget-object v0, p1, LX/08Wb;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777223
    .line 16777224
    if-nez v0, :cond_b

    .line 16777225
    .line 16777226
    return-void

    .line 16777227
    :cond_b
    iput-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777228
    .line 16777229
    invoke-virtual {p0, v0}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777230
    .line 16777231
    .line 16777232
    iget-object v0, p1, LX/08Wb;->LIZIZ:Ljava/lang/String;

    .line 16777233
    .line 16777234
    if-nez v0, :cond_16

    .line 16777235
    .line 16777236
    const-string v0, ""

    .line 16777237
    .line 16777238
    :cond_16
    iput-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777239
    .line 16777240
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777241
    .line 16777242
    if-nez v0, :cond_4e

    .line 16777243
    .line 16777244
    invoke-static {}, LX/092h;->LIZ()Landroid/app/Activity;

    .line 16777245
    .line 16777246
    .line 16777247
    move-result-object v4

    .line 16777248
    instance-of v0, v4, LX/02IC;

    .line 16777249
    .line 16777250
    const/4 v3, 0x0

    .line 16777251
    if-eqz v0, :cond_4e

    .line 16777252
    .line 16777253
    check-cast v4, LX/02IC;

    .line 16777254
    .line 16777255
    if-eqz v4, :cond_4e

    .line 16777256
    .line 16777257
    sget-object v2, Lcom/bytedance/hox/Hox;->LLJLILLLLZIIL:LX/06SY;

    .line 16777258
    .line 16777259
    invoke-virtual {v2, v4}, LX/06SY;->LIZ(LX/02IC;)Lcom/bytedance/hox/Hox;

    .line 16777260
    .line 16777261
    .line 16777262
    move-result-object v0

    .line 16777263
    const-string v1, "For You"

    .line 16777264
    .line 16777265
    invoke-virtual {v0, v1}, Lcom/bytedance/hox/Hox;->p53(Ljava/lang/String;)Z

    .line 16777266
    .line 16777267
    .line 16777268
    move-result v0

    .line 16777269
    if-eqz v0, :cond_4e

    .line 16777270
    .line 16777271
    invoke-virtual {v2, v4}, LX/06SY;->LIZ(LX/02IC;)Lcom/bytedance/hox/Hox;

    .line 16777272
    .line 16777273
    .line 16777274
    move-result-object v0

    .line 16777275
    invoke-virtual {v0, v1}, Lcom/bytedance/hox/Hox;->Oj(Ljava/lang/String;)Landroidx/fragment/app/Fragment;

    .line 16777276
    .line 16777277
    .line 16777278
    move-result-object v0

    .line 16777279
    if-eqz v0, :cond_53

    .line 16777280
    .line 16777281
    invoke-static {v0}, LX/06OA;->LIZJ(Landroidx/fragment/app/Fragment;)Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777282
    .line 16777283
    .line 16777284
    move-result-object v1

    .line 16777285
    :goto_45
    instance-of v0, v1, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777286
    .line 16777287
    if-eqz v0, :cond_4c

    .line 16777288
    .line 16777289
    move-object v3, v1

    .line 16777290
    check-cast v3, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777291
    .line 16777292
    :cond_4c
    iput-object v3, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777293
    .line 16777294
    :cond_4e
    const/4 v0, 0x1

    .line 16777295
    invoke-virtual {p0, v0}, LX/08Vk;->LJJI(Z)V

    .line 16777296
    .line 16777297
    .line 16777298
    :cond_52
    return-void

    .line 16777299
    :cond_53
    move-object v1, v3

    .line 16777300
    goto :goto_45
.end method

.method public final onFullFeedFragmentPageStateChangeEvent(LX/08Wd;)V
    .registers 5
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    invoke-virtual {p0, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777217
    .line 16777218
    .line 16777219
    move-result v0

    .line 16777220
    if-eqz v0, :cond_16

    .line 16777221
    .line 16777222
    iget-object v0, p1, LX/08Wd;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777223
    .line 16777224
    invoke-virtual {p0, v0}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777225
    .line 16777226
    .line 16777227
    move-result v0

    .line 16777228
    if-eqz v0, :cond_16

    .line 16777229
    .line 16777230
    iget-object v2, p1, LX/08Wd;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777231
    .line 16777232
    iget v1, p1, LX/08Wd;->LIZIZ:I

    .line 16777233
    .line 16777234
    const/4 v0, 0x1

    .line 16777235
    invoke-virtual {p0, v2, v1, v0}, LX/08Vk;->LJJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;IZ)V

    .line 16777236
    .line 16777237
    .line 16777238
    :cond_16
    return-void
.end method

.method public final onFullFeedVideoChangeEvent(LX/08Wf;)V
    .registers 6
    .annotation runtime LX/04GW;
        threadMode = .enum Lorg/greenrobot/eventbus/ThreadMode;->MAIN:Lorg/greenrobot/eventbus/ThreadMode;
    .end annotation

    .prologue
    .line 16777216
    iget-object v3, p1, LX/08Wf;->LIZLLL:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777217
    .line 16777218
    iget-object v2, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777219
    .line 16777220
    invoke-static {}, LX/02nu;->LIZ()Ljava/lang/StringBuilder;

    .line 16777221
    .line 16777222
    .line 16777223
    move-result-object v1

    .line 16777224
    const-string v0, "onFullFeedVideoChangeEvent, mIsTrackingTouch="

    .line 16777225
    .line 16777226
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777227
    .line 16777228
    .line 16777229
    iget-boolean v0, p0, LX/08Vk;->LLLF:Z

    .line 16777230
    .line 16777231
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    .line 16777232
    .line 16777233
    .line 16777234
    const-string v0, ", panel.pageResumed="

    .line 16777235
    .line 16777236
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777237
    .line 16777238
    .line 16777239
    invoke-interface {v3}, LX/05fP;->LJFF()Z

    .line 16777240
    .line 16777241
    .line 16777242
    move-result v0

    .line 16777243
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    .line 16777244
    .line 16777245
    .line 16777246
    invoke-static {v1}, LX/02nu;->LIZIZ(Ljava/lang/StringBuilder;)Ljava/lang/String;

    .line 16777247
    .line 16777248
    .line 16777249
    move-result-object v1

    .line 16777250
    iget-object v0, p1, LX/08Wf;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777251
    .line 16777252
    invoke-virtual {v2, v1, v0}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777253
    .line 16777254
    .line 16777255
    invoke-interface {v3}, LX/05fP;->LJFF()Z

    .line 16777256
    .line 16777257
    .line 16777258
    move-result v0

    .line 16777259
    if-nez v0, :cond_2e

    .line 16777260
    .line 16777261
    return-void

    .line 16777262
    :cond_2e
    invoke-virtual {p0, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777263
    .line 16777264
    .line 16777265
    move-result v0

    .line 16777266
    if-eqz v0, :cond_7c

    .line 16777267
    .line 16777268
    iget-object v1, p1, LX/08Wf;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777269
    .line 16777270
    iput-object v1, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777271
    .line 16777272
    iget-object v0, p1, LX/08Wf;->LIZIZ:Ljava/lang/String;

    .line 16777273
    .line 16777274
    if-nez v0, :cond_3e

    .line 16777275
    .line 16777276
    const-string v0, ""

    .line 16777277
    .line 16777278
    :cond_3e
    iput-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777279
    .line 16777280
    iget v0, p1, LX/08Wf;->LIZJ:I

    .line 16777281
    .line 16777282
    iput v0, p0, LX/08Vk;->LLLIIIIL:I

    .line 16777283
    .line 16777284
    iput-object v3, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777285
    .line 16777286
    iget-boolean v0, p0, LX/08Vk;->LLLIILIL:Z

    .line 16777287
    .line 16777288
    if-nez v0, :cond_4d

    .line 16777289
    .line 16777290
    const/4 v0, 0x0

    .line 16777291
    iput-boolean v0, p0, LX/08Vk;->LLLILZ:Z

    .line 16777292
    .line 16777293
    :cond_4d
    sget-boolean v0, LX/0A50;->LJIIIZ:Z

    .line 16777294
    .line 16777295
    if-eqz v0, :cond_5c

    .line 16777296
    .line 16777297
    invoke-static {v1}, LX/0A0T;->LIZIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777298
    .line 16777299
    .line 16777300
    move-result v0

    .line 16777301
    if-eqz v0, :cond_5c

    .line 16777302
    .line 16777303
    iget-object v0, p1, LX/08Wf;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777304
    .line 16777305
    invoke-virtual {p0, v0}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777306
    .line 16777307
    .line 16777308
    :cond_5c
    iget-object v0, p1, LX/08Wf;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777309
    .line 16777310
    invoke-static {v0}, LX/0A7u;->LJIL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777311
    .line 16777312
    .line 16777313
    move-result v0

    .line 16777314
    if-eqz v0, :cond_69

    .line 16777315
    .line 16777316
    iget-object v0, p1, LX/08Wf;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777317
    .line 16777318
    invoke-virtual {p0, v0}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777319
    .line 16777320
    .line 16777321
    :cond_69
    invoke-static {}, LX/0AFa;->LIZ()Z

    .line 16777322
    .line 16777323
    .line 16777324
    move-result v0

    .line 16777325
    if-eqz v0, :cond_78

    .line 16777326
    .line 16777327
    iget-object v0, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777328
    .line 16777329
    if-nez v0, :cond_78

    .line 16777330
    .line 16777331
    iget-object v0, p1, LX/08Wf;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777332
    .line 16777333
    invoke-virtual {p0, v0}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777334
    .line 16777335
    .line 16777336
    :cond_78
    const/4 v0, 0x1

    .line 16777337
    invoke-virtual {p0, v0}, LX/08Vk;->LJJI(Z)V

    .line 16777338
    .line 16777339
    .line 16777340
    :cond_7c
    return-void
.end method

.method public final onInnerOuterFeedAwemeResetEvent(LX/08Wh;)V
    .registers 3
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    iget-object v0, p1, LX/08Wh;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777217
    .line 16777218
    if-nez v0, :cond_5

    .line 16777219
    .line 16777220
    return-void

    .line 16777221
    :cond_5
    iput-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777222
    .line 16777223
    invoke-virtual {p0, v0}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777224
    .line 16777225
    .line 16777226
    return-void
.end method

.method public final onPlayerControllerVideoPlayProgressChange(LX/08Wo;)V
    .registers 10
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    iget-boolean v2, p1, LX/08Wo;->LJI:Z

    .line 16777217
    .line 16777218
    move-object v1, p0

    .line 16777219
    invoke-virtual {v1, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777220
    .line 16777221
    .line 16777222
    move-result v3

    .line 16777223
    iget-object v4, p1, LX/08Wo;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777224
    .line 16777225
    iget v5, p1, LX/08Wo;->LIZJ:F

    .line 16777226
    .line 16777227
    iget-object v6, p1, LX/08Wo;->LIZIZ:Ljava/lang/String;

    .line 16777228
    .line 16777229
    iget-object v0, p1, LX/08Wo;->LIZLLL:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777230
    .line 16777231
    if-eqz v0, :cond_1d

    .line 16777232
    .line 16777233
    invoke-interface {v0}, LX/05fP;->getUserVisibleHint()Z

    .line 16777234
    .line 16777235
    .line 16777236
    move-result v0

    .line 16777237
    :goto_15
    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 16777238
    .line 16777239
    .line 16777240
    move-result-object v7

    .line 16777241
    :goto_19
    invoke-virtual/range {v1 .. v7}, LX/08Vk;->LJIL(ZZLcom/ss/android/ugc/aweme/feed/model/Aweme;FLjava/lang/String;Ljava/lang/Boolean;)V

    .line 16777242
    .line 16777243
    .line 16777244
    return-void

    .line 16777245
    :cond_1d
    iget-object v0, p1, LX/08Wo;->LJ:Ljava/lang/ref/WeakReference;

    .line 16777246
    .line 16777247
    if-eqz v0, :cond_2e

    .line 16777248
    .line 16777249
    invoke-virtual {v0}, Ljava/lang/ref/Reference;->get()Ljava/lang/Object;

    .line 16777250
    .line 16777251
    .line 16777252
    move-result-object v0

    .line 16777253
    check-cast v0, LX/05fP;

    .line 16777254
    .line 16777255
    if-eqz v0, :cond_2e

    .line 16777256
    .line 16777257
    invoke-interface {v0}, LX/05fP;->getUserVisibleHint()Z

    .line 16777258
    .line 16777259
    .line 16777260
    move-result v0

    .line 16777261
    goto :goto_15

    .line 16777262
    :cond_2e
    const/4 v7, 0x0

    .line 16777263
    goto :goto_19
.end method

.method public final onPlayerControllerVideoStatusEvent(LX/08Wp;)V
    .registers 6
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    iget-boolean v0, p1, LX/08Wp;->LJ:Z

    .line 16777217
    .line 16777218
    iput-boolean v0, p0, LX/08Vk;->LLLILZLLLI:Z

    .line 16777219
    .line 16777220
    invoke-virtual {p0, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777221
    .line 16777222
    .line 16777223
    move-result v0

    .line 16777224
    if-eqz v0, :cond_31

    .line 16777225
    .line 16777226
    iget-object v0, p1, LX/08Wp;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777227
    .line 16777228
    invoke-virtual {p0, v0}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777229
    .line 16777230
    .line 16777231
    move-result v0

    .line 16777232
    if-eqz v0, :cond_31

    .line 16777233
    .line 16777234
    iget v3, p1, LX/08Wp;->LIZJ:I

    .line 16777235
    .line 16777236
    const/4 v2, 0x0

    .line 16777237
    const/4 v1, 0x1

    .line 16777238
    if-eq v3, v1, :cond_32

    .line 16777239
    .line 16777240
    const/4 v0, 0x2

    .line 16777241
    if-ne v3, v0, :cond_31

    .line 16777242
    .line 16777243
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777244
    .line 16777245
    invoke-virtual {v0, v2}, LX/08Ic;->setPauseStatus(Z)V

    .line 16777246
    .line 16777247
    .line 16777248
    iget-boolean v0, p0, LX/08Vk;->LLLIILIL:Z

    .line 16777249
    .line 16777250
    if-eqz v0, :cond_31

    .line 16777251
    .line 16777252
    iget-boolean v0, p0, LX/08Vk;->LLLIL:Z

    .line 16777253
    .line 16777254
    if-nez v0, :cond_31

    .line 16777255
    .line 16777256
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777257
    .line 16777258
    invoke-virtual {p0, v2}, LX/08Vk;->LJIIJJI(Z)I

    .line 16777259
    .line 16777260
    .line 16777261
    move-result v0

    .line 16777262
    invoke-virtual {v1, v0}, LX/08Ic;->setSeekBarShowType(I)V

    .line 16777263
    .line 16777264
    .line 16777265
    :cond_31
    return-void

    .line 16777266
    :cond_32
    iget-boolean v0, p0, LX/08Vk;->LLLILZLLLI:Z

    .line 16777267
    .line 16777268
    if-eqz v0, :cond_3b

    .line 16777269
    .line 16777270
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777271
    .line 16777272
    invoke-static {v0, v2}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777273
    .line 16777274
    .line 16777275
    :cond_3b
    iput v2, p0, LX/08Vk;->LLLILZJ:I

    .line 16777276
    .line 16777277
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777278
    .line 16777279
    invoke-virtual {v0, v1}, LX/08Ic;->setPauseStatus(Z)V

    .line 16777280
    .line 16777281
    .line 16777282
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777283
    .line 16777284
    invoke-virtual {v0, v1}, LX/08Ic;->setSeekBarShowType(I)V

    .line 16777285
    .line 16777286
    .line 16777287
    iget-object v1, p0, LX/08Vk;->LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 16777288
    .line 16777289
    iget-object v0, p0, LX/08Vk;->LLLLILI:LX/08Xl;

    .line 16777290
    .line 16777291
    invoke-virtual {v1, v0}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 16777292
    .line 16777293
    .line 16777294
    iput-boolean v2, p0, LX/08Vk;->LLLIL:Z

    .line 16777295
    .line 16777296
    return-void
.end method

.method public onProgressChanged(Landroid/widget/SeekBar;IZ)V
    .registers 10

    .prologue
    .line 50331648
    iget-boolean v0, p0, LX/08Vk;->LLLF:Z

    .line 50331649
    .line 50331650
    if-eqz v0, :cond_4d

    .line 50331651
    .line 50331652
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 50331653
    .line 50331654
    .line 50331655
    move-result-object v0

    .line 50331656
    invoke-virtual {v0, p2}, LX/08WY;->LIZLLL(I)V

    .line 50331657
    .line 50331658
    .line 50331659
    invoke-virtual {p0}, LX/08Vk;->LJIILL()Z

    .line 50331660
    .line 50331661
    .line 50331662
    move-result v0

    .line 50331663
    if-eqz v0, :cond_4d

    .line 50331664
    .line 50331665
    iget-object v0, p0, LX/08Vk;->LLJLLL:Ljava/util/List;

    .line 50331666
    .line 50331667
    check-cast v0, Ljava/util/ArrayList;

    .line 50331668
    .line 50331669
    invoke-virtual {v0}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    .line 50331670
    .line 50331671
    .line 50331672
    move-result-object v4

    .line 50331673
    const/4 v5, 0x0

    .line 50331674
    :goto_1a
    invoke-interface {v4}, Ljava/util/Iterator;->hasNext()Z

    .line 50331675
    .line 50331676
    .line 50331677
    move-result v0

    .line 50331678
    if-eqz v0, :cond_33

    .line 50331679
    .line 50331680
    invoke-interface {v4}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 50331681
    .line 50331682
    .line 50331683
    move-result-object v3

    .line 50331684
    check-cast v3, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;

    .line 50331685
    .line 50331686
    iget v2, v3, Lcom/ss/android/ugc/aweme/search/innerflowpage/SearchKeyFramePoint;->LIZIZ:F

    .line 50331687
    .line 50331688
    const/high16 v1, 0x42c80000    # 100.0f

    .line 50331689
    .line 50331690
    mul-float/2addr v2, v1

    .line 50331691
    int-to-float v0, p2

    .line 50331692
    div-float/2addr v0, v1

    .line 50331693
    cmpl-float v0, v2, v0

    .line 50331694
    .line 50331695
    if-gtz v0, :cond_33

    .line 50331696
    .line 50331697
    move-object v5, v3

    .line 50331698
    goto :goto_1a

    .line 50331699
    :cond_33
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 50331700
    .line 50331701
    .line 50331702
    move-result-object v0

    .line 50331703
    iget-object v4, v0, LX/08WY;->LIZ:[LX/08Wt;

    .line 50331704
    .line 50331705
    array-length v3, v4

    .line 50331706
    const/4 v2, 0x0

    .line 50331707
    :goto_3b
    if-ge v2, v3, :cond_4d

    .line 50331708
    .line 50331709
    aget-object v0, v4, v2

    .line 50331710
    .line 50331711
    iget-object v1, v0, LX/08Wt;->LIZJ:LX/08XE;

    .line 50331712
    .line 50331713
    if-eqz v1, :cond_4a

    .line 50331714
    .line 50331715
    invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 50331716
    .line 50331717
    .line 50331718
    move-result-object v0

    .line 50331719
    invoke-virtual {v1, v0, v5}, LX/08XE;->invoke(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 50331720
    .line 50331721
    .line 50331722
    :cond_4a
    add-int/lit8 v2, v2, 0x1

    .line 50331723
    .line 50331724
    goto :goto_3b

    .line 50331725
    :cond_4d
    return-void
.end method

.method public final onRenderFirstFrame(LX/06I5;)V
    .registers 7
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    invoke-virtual {p0, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777217
    .line 16777218
    .line 16777219
    move-result v0

    .line 16777220
    if-eqz v0, :cond_68

    .line 16777221
    .line 16777222
    iget-object v4, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777223
    .line 16777224
    iget-object v0, p1, LX/06I5;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777225
    .line 16777226
    invoke-virtual {p0, v0}, LX/08Vk;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777227
    .line 16777228
    .line 16777229
    iget-object v2, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777230
    .line 16777231
    const-string v1, "onRenderFirstFrame, set renderAweme"

    .line 16777232
    .line 16777233
    iget-object v0, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777234
    .line 16777235
    invoke-virtual {v2, v1, v0}, LX/05f6;->LJIIJJI(Ljava/lang/String;Ljava/lang/Object;)V

    .line 16777236
    .line 16777237
    .line 16777238
    sget-object v0, LX/0BXB;->LIZ:LX/02pj;

    .line 16777239
    .line 16777240
    invoke-interface {v0}, LX/02pj;->getValue()Ljava/lang/Object;

    .line 16777241
    .line 16777242
    .line 16777243
    move-result-object v0

    .line 16777244
    check-cast v0, Ljava/lang/Boolean;

    .line 16777245
    .line 16777246
    invoke-virtual {v0}, Ljava/lang/Boolean;->booleanValue()Z

    .line 16777247
    .line 16777248
    .line 16777249
    move-result v0

    .line 16777250
    const/4 v3, 0x1

    .line 16777251
    const/4 v2, 0x0

    .line 16777252
    if-eqz v0, :cond_6e

    .line 16777253
    .line 16777254
    if-eqz v4, :cond_6c

    .line 16777255
    .line 16777256
    invoke-virtual {v4}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777257
    .line 16777258
    .line 16777259
    move-result-object v1

    .line 16777260
    :goto_2c
    iget-object v0, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777261
    .line 16777262
    if-eqz v0, :cond_34

    .line 16777263
    .line 16777264
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777265
    .line 16777266
    .line 16777267
    move-result-object v2

    .line 16777268
    :cond_34
    invoke-static {v1, v2}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z

    .line 16777269
    .line 16777270
    .line 16777271
    move-result v0

    .line 16777272
    if-eqz v0, :cond_3d

    .line 16777273
    .line 16777274
    invoke-virtual {p0, v3}, LX/08Vk;->LJJI(Z)V

    .line 16777275
    .line 16777276
    .line 16777277
    :cond_3d
    :goto_3d
    const/4 v0, 0x0

    .line 16777278
    iput v0, p0, LX/08Vk;->LLLJL:I

    .line 16777279
    .line 16777280
    iget-object v2, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777281
    .line 16777282
    iget-object v1, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777283
    .line 16777284
    if-eqz v2, :cond_69

    .line 16777285
    .line 16777286
    invoke-virtual {v2}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777287
    .line 16777288
    .line 16777289
    move-result-object v0

    .line 16777290
    if-eqz v0, :cond_69

    .line 16777291
    .line 16777292
    sget-object v0, LX/0AFd;->LJI:Ljava/util/HashMap;

    .line 16777293
    .line 16777294
    invoke-virtual {v0, v1}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 16777295
    .line 16777296
    .line 16777297
    move-result-object v1

    .line 16777298
    check-cast v1, Landroid/util/LruCache;

    .line 16777299
    .line 16777300
    if-eqz v1, :cond_69

    .line 16777301
    .line 16777302
    invoke-virtual {v2}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777303
    .line 16777304
    .line 16777305
    move-result-object v0

    .line 16777306
    invoke-virtual {v1, v0}, Landroid/util/LruCache;->get(Ljava/lang/Object;)Ljava/lang/Object;

    .line 16777307
    .line 16777308
    .line 16777309
    move-result-object v0

    .line 16777310
    check-cast v0, Ljava/lang/Long;

    .line 16777311
    .line 16777312
    if-eqz v0, :cond_69

    .line 16777313
    .line 16777314
    invoke-virtual {v0}, Ljava/lang/Long;->longValue()J

    .line 16777315
    .line 16777316
    .line 16777317
    move-result-wide v0

    .line 16777318
    :goto_66
    iput-wide v0, p0, LX/08Vk;->LLLL:J

    .line 16777319
    .line 16777320
    :cond_68
    return-void

    .line 16777321
    :cond_69
    const-wide/16 v0, 0x0

    .line 16777322
    .line 16777323
    goto :goto_66

    .line 16777324
    :cond_6c
    move-object v1, v2

    .line 16777325
    goto :goto_2c

    .line 16777326
    :cond_6e
    invoke-virtual {p0, v3}, LX/08Vk;->LJJI(Z)V

    .line 16777327
    .line 16777328
    .line 16777329
    goto :goto_3d
.end method

.method public final onStartTrackingTouch(Landroid/widget/SeekBar;)V
    .registers 17

    .prologue
    .line 16777216
    const/4 v2, 0x1

    .line 16777217
    iput-boolean v2, p0, LX/08Vk;->LLLF:Z

    .line 16777218
    .line 16777219
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 16777220
    .line 16777221
    .line 16777222
    move-result-object v1

    .line 16777223
    const-class v0, Lcom/ss/android/ugc/aweme/feed/FypAutoScrollService;

    .line 16777224
    .line 16777225
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 16777226
    .line 16777227
    .line 16777228
    move-result-object v0

    .line 16777229
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/FypAutoScrollService;

    .line 16777230
    .line 16777231
    invoke-interface {v0, v2}, Lcom/ss/android/ugc/aweme/feed/FypAutoScrollService;->LJIIL(Z)V

    .line 16777232
    .line 16777233
    .line 16777234
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777235
    .line 16777236
    const/4 v8, 0x0

    .line 16777237
    if-eqz v0, :cond_36

    .line 16777238
    .line 16777239
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777240
    .line 16777241
    .line 16777242
    move-result-object v0

    .line 16777243
    if-eqz v0, :cond_36

    .line 16777244
    .line 16777245
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16777246
    .line 16777247
    .line 16777248
    move-result-object v1

    .line 16777249
    if-eqz v1, :cond_36

    .line 16777250
    .line 16777251
    const-class v0, Lcom/ss/android/ugc/feed/platform/panel/midad/IMidAdAbility;

    .line 16777252
    .line 16777253
    invoke-static {v1, v0, v8}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777254
    .line 16777255
    .line 16777256
    move-result-object v0

    .line 16777257
    check-cast v0, Lcom/ss/android/ugc/feed/platform/panel/midad/IMidAdAbility;

    .line 16777258
    .line 16777259
    if-eqz v0, :cond_36

    .line 16777260
    .line 16777261
    invoke-interface {v0}, Lcom/ss/android/ugc/feed/platform/panel/midad/IMidAdAbility;->Bv0()Lcom/ss/android/ugc/aweme/ad/feed/midad/IMidAdCellAbility;

    .line 16777262
    .line 16777263
    .line 16777264
    move-result-object v0

    .line 16777265
    if-eqz v0, :cond_36

    .line 16777266
    .line 16777267
    invoke-interface {v0, v2}, Lcom/ss/android/ugc/aweme/ad/feed/midad/IMidAdCellAbility;->Ff1(Z)V

    .line 16777268
    .line 16777269
    .line 16777270
    :cond_36
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777271
    .line 16777272
    const/4 v7, 0x0

    .line 16777273
    if-eqz v0, :cond_a4

    .line 16777274
    .line 16777275
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->LJLJJLL()LX/08BV;

    .line 16777276
    .line 16777277
    .line 16777278
    move-result-object v0

    .line 16777279
    if-eqz v0, :cond_a4

    .line 16777280
    .line 16777281
    invoke-static {v0}, LX/0CB5;->LIZIZ(LX/08BV;)LX/03so;

    .line 16777282
    .line 16777283
    .line 16777284
    move-result-object v1

    .line 16777285
    if-eqz v1, :cond_a4

    .line 16777286
    .line 16777287
    const-class v0, Lcom/ss/android/ugc/aweme/feed/collection/collection/ability/UserFeedViewPagerAbility;

    .line 16777288
    .line 16777289
    invoke-static {v1, v0, v8}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777290
    .line 16777291
    .line 16777292
    move-result-object v0

    .line 16777293
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/collection/collection/ability/UserFeedViewPagerAbility;

    .line 16777294
    .line 16777295
    if-eqz v0, :cond_a4

    .line 16777296
    .line 16777297
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/collection/collection/ability/UserFeedViewPagerAbility;->kT1()LX/0BHY;

    .line 16777298
    .line 16777299
    .line 16777300
    move-result-object v3

    .line 16777301
    if-eqz v3, :cond_a4

    .line 16777302
    .line 16777303
    instance-of v0, v3, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777304
    .line 16777305
    if-eqz v0, :cond_72

    .line 16777306
    .line 16777307
    move-object v0, v3

    .line 16777308
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777309
    .line 16777310
    if-eqz v0, :cond_72

    .line 16777311
    .line 16777312
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;->rN2()LX/02xX;

    .line 16777313
    .line 16777314
    .line 16777315
    move-result-object v0

    .line 16777316
    if-eqz v0, :cond_72

    .line 16777317
    .line 16777318
    invoke-static {v0}, LX/06GI;->LIZIZ(LX/02xX;)LX/03so;

    .line 16777319
    .line 16777320
    .line 16777321
    move-result-object v1

    .line 16777322
    const-class v0, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;

    .line 16777323
    .line 16777324
    invoke-static {v1, v0, v7}, LX/06Cb;->LIZIZ(LX/03so;Ljava/lang/Class;Z)Ljava/util/List;

    .line 16777325
    .line 16777326
    .line 16777327
    move-result-object v0

    .line 16777328
    if-nez v0, :cond_90

    .line 16777329
    .line 16777330
    :cond_72
    invoke-interface {v3}, LX/08BV;->q1()LX/08BV;

    .line 16777331
    .line 16777332
    .line 16777333
    move-result-object v1

    .line 16777334
    instance-of v0, v1, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777335
    .line 16777336
    if-eqz v0, :cond_a4

    .line 16777337
    .line 16777338
    check-cast v1, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777339
    .line 16777340
    if-eqz v1, :cond_a4

    .line 16777341
    .line 16777342
    invoke-interface {v1}, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;->rN2()LX/02xX;

    .line 16777343
    .line 16777344
    .line 16777345
    move-result-object v0

    .line 16777346
    if-eqz v0, :cond_a4

    .line 16777347
    .line 16777348
    invoke-static {v0}, LX/06GI;->LIZIZ(LX/02xX;)LX/03so;

    .line 16777349
    .line 16777350
    .line 16777351
    move-result-object v1

    .line 16777352
    const-class v0, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;

    .line 16777353
    .line 16777354
    invoke-static {v1, v0, v7}, LX/06Cb;->LIZIZ(LX/03so;Ljava/lang/Class;Z)Ljava/util/List;

    .line 16777355
    .line 16777356
    .line 16777357
    move-result-object v0

    .line 16777358
    if-eqz v0, :cond_a4

    .line 16777359
    .line 16777360
    :cond_90
    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 16777361
    .line 16777362
    .line 16777363
    move-result-object v1

    .line 16777364
    :goto_94
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 16777365
    .line 16777366
    .line 16777367
    move-result v0

    .line 16777368
    if-eqz v0, :cond_a4

    .line 16777369
    .line 16777370
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 16777371
    .line 16777372
    .line 16777373
    move-result-object v0

    .line 16777374
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;

    .line 16777375
    .line 16777376
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;->tx()V

    .line 16777377
    .line 16777378
    .line 16777379
    goto :goto_94

    .line 16777380
    :cond_a4
    new-instance v1, LX/09ei;

    .line 16777381
    .line 16777382
    const/4 v0, 0x4

    .line 16777383
    invoke-direct {v1, v2, v7, v0}, LX/09ei;-><init>(ZZI)V

    .line 16777384
    .line 16777385
    .line 16777386
    invoke-static {v1}, LX/096q;->LIZ(Lcom/ss/android/ugc/governance/eventbus/IEvent;)Lcom/ss/android/ugc/governance/eventbus/IEvent;

    .line 16777387
    .line 16777388
    .line 16777389
    if-eqz p1, :cond_23b

    .line 16777390
    .line 16777391
    invoke-virtual/range {p1 .. p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16777392
    .line 16777393
    .line 16777394
    move-result v0

    .line 16777395
    :goto_b3
    iput v0, p0, LX/08Vk;->LLLIZZ:I

    .line 16777396
    .line 16777397
    iget-boolean v0, p0, LX/08Vk;->LLLJIL:Z

    .line 16777398
    .line 16777399
    if-nez v0, :cond_c8

    .line 16777400
    .line 16777401
    iget-object v3, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777402
    .line 16777403
    invoke-virtual {p0}, LX/08Vk;->LJIIL()Ljava/lang/String;

    .line 16777404
    .line 16777405
    .line 16777406
    move-result-object v1

    .line 16777407
    sget-boolean v0, LX/05f6;->LIZLLL:Z

    .line 16777408
    .line 16777409
    const-string v0, "attach drag hint view"

    .line 16777410
    .line 16777411
    invoke-virtual {v3, v8, v1, v0}, LX/05f6;->LJIIIZ(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V

    .line 16777412
    .line 16777413
    .line 16777414
    iput-boolean v2, p0, LX/08Vk;->LLLJIL:Z

    .line 16777415
    .line 16777416
    :cond_c8
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 16777417
    .line 16777418
    .line 16777419
    move-result v0

    .line 16777420
    if-eqz v0, :cond_d5

    .line 16777421
    .line 16777422
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777423
    .line 16777424
    if-eqz v0, :cond_d5

    .line 16777425
    .line 16777426
    invoke-interface {v0}, LX/05fN;->hideIvPlay()V

    .line 16777427
    .line 16777428
    .line 16777429
    :cond_d5
    if-eqz p1, :cond_dd

    .line 16777430
    .line 16777431
    invoke-virtual/range {p1 .. p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16777432
    .line 16777433
    .line 16777434
    move-result v0

    .line 16777435
    iput v0, p0, LX/08Vk;->LLLIIIL:I

    .line 16777436
    .line 16777437
    :cond_dd
    iget-object v3, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777438
    .line 16777439
    invoke-static {}, LX/02nu;->LIZ()Ljava/lang/StringBuilder;

    .line 16777440
    .line 16777441
    .line 16777442
    move-result-object v1

    .line 16777443
    const-string v0, "onStartTrackingTouch, start progress="

    .line 16777444
    .line 16777445
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777446
    .line 16777447
    .line 16777448
    iget v0, p0, LX/08Vk;->LLLIIIL:I

    .line 16777449
    .line 16777450
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    .line 16777451
    .line 16777452
    .line 16777453
    invoke-static {v1}, LX/02nu;->LIZIZ(Ljava/lang/StringBuilder;)Ljava/lang/String;

    .line 16777454
    .line 16777455
    .line 16777456
    move-result-object v0

    .line 16777457
    invoke-virtual {v3, v0}, LX/05f6;->LJ(Ljava/lang/String;)V

    .line 16777458
    .line 16777459
    .line 16777460
    iget-object v1, p0, LX/08Vk;->LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 16777461
    .line 16777462
    iget-object v0, p0, LX/08Vk;->LLLLILI:LX/08Xl;

    .line 16777463
    .line 16777464
    invoke-virtual {v1, v0}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 16777465
    .line 16777466
    .line 16777467
    iget-object v5, p0, LX/08Vk;->LLJJJJ:Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;

    .line 16777468
    .line 16777469
    if-eqz v5, :cond_187

    .line 16777470
    .line 16777471
    iget-object v9, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777472
    .line 16777473
    iget-object v3, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777474
    .line 16777475
    const-string/jumbo v6, "seek_guide_shown_count"

    .line 16777476
    .line 16777477
    .line 16777478
    const-string v4, "last_seek_guide_shown_time"

    .line 16777479
    .line 16777480
    if-eqz v9, :cond_187

    .line 16777481
    .line 16777482
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 16777483
    .line 16777484
    .line 16777485
    move-result-wide v13

    .line 16777486
    invoke-static {}, LX/0AAT;->LIZ()Z

    .line 16777487
    .line 16777488
    .line 16777489
    move-result v0

    .line 16777490
    if-eqz v0, :cond_187

    .line 16777491
    .line 16777492
    invoke-static {}, LX/0Bwf;->LIZIZ()Z

    .line 16777493
    .line 16777494
    .line 16777495
    move-result v0

    .line 16777496
    if-eqz v0, :cond_187

    .line 16777497
    .line 16777498
    sget-object v0, LX/0A4m;->LIZ:LX/0A4m;

    .line 16777499
    .line 16777500
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777501
    .line 16777502
    .line 16777503
    invoke-static {v9}, LX/0A4m;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777504
    .line 16777505
    .line 16777506
    move-result v0

    .line 16777507
    if-eqz v0, :cond_187

    .line 16777508
    .line 16777509
    if-eqz v3, :cond_238

    .line 16777510
    .line 16777511
    invoke-interface {v3}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->getEventType()Ljava/lang/String;

    .line 16777512
    .line 16777513
    .line 16777514
    move-result-object v1

    .line 16777515
    :goto_12b
    const-string v0, "homepage_hot"

    .line 16777516
    .line 16777517
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777518
    .line 16777519
    .line 16777520
    move-result v0

    .line 16777521
    if-eqz v0, :cond_187

    .line 16777522
    .line 16777523
    invoke-virtual {v9}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777524
    .line 16777525
    .line 16777526
    move-result-object v0

    .line 16777527
    if-eqz v0, :cond_235

    .line 16777528
    .line 16777529
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777530
    .line 16777531
    .line 16777532
    move-result v0

    .line 16777533
    :goto_13d
    invoke-static {v0}, LX/0BX3;->LIZJ(I)I

    .line 16777534
    .line 16777535
    .line 16777536
    move-result v1

    .line 16777537
    const/16 v0, 0x1e

    .line 16777538
    .line 16777539
    if-lt v1, v0, :cond_187

    .line 16777540
    .line 16777541
    invoke-static {}, LX/0Bwe;->LIZ()J

    .line 16777542
    .line 16777543
    .line 16777544
    move-result-wide v0

    .line 16777545
    sub-long v11, v13, v0

    .line 16777546
    .line 16777547
    const-wide/32 v9, 0xf731400

    .line 16777548
    .line 16777549
    .line 16777550
    cmp-long v0, v11, v9

    .line 16777551
    .line 16777552
    if-ltz v0, :cond_187

    .line 16777553
    .line 16777554
    sget-object v11, LX/0Bwe;->LIZ:Lcom/bytedance/keva/Keva;

    .line 16777555
    .line 16777556
    const-wide/16 v0, 0x0

    .line 16777557
    .line 16777558
    invoke-virtual {v11, v4, v0, v1}, Lcom/bytedance/keva/Keva;->getLong(Ljava/lang/String;J)J

    .line 16777559
    .line 16777560
    .line 16777561
    move-result-wide v0

    .line 16777562
    sub-long/2addr v13, v0

    .line 16777563
    const-wide/32 v9, 0x5265c00

    .line 16777564
    .line 16777565
    .line 16777566
    cmp-long v0, v13, v9

    .line 16777567
    .line 16777568
    if-ltz v0, :cond_187

    .line 16777569
    .line 16777570
    invoke-virtual {v11, v6, v7}, Lcom/bytedance/keva/Keva;->getInt(Ljava/lang/String;I)I

    .line 16777571
    .line 16777572
    .line 16777573
    move-result v1

    .line 16777574
    const/4 v0, 0x3

    .line 16777575
    if-ge v1, v0, :cond_187

    .line 16777576
    .line 16777577
    if-eqz v3, :cond_1a5

    .line 16777578
    .line 16777579
    invoke-interface {v3}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777580
    .line 16777581
    .line 16777582
    move-result-object v0

    .line 16777583
    if-eqz v0, :cond_188

    .line 16777584
    .line 16777585
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16777586
    .line 16777587
    .line 16777588
    move-result-object v1

    .line 16777589
    if-eqz v1, :cond_188

    .line 16777590
    .line 16777591
    const-class v0, Lcom/ss/android/ugc/aweme/base/INonPersonalizedAbility;

    .line 16777592
    .line 16777593
    invoke-static {v1, v0, v8}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777594
    .line 16777595
    .line 16777596
    move-result-object v0

    .line 16777597
    check-cast v0, Lcom/ss/android/ugc/aweme/base/INonPersonalizedAbility;

    .line 16777598
    .line 16777599
    if-eqz v0, :cond_188

    .line 16777600
    .line 16777601
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/base/INonPersonalizedAbility;->wt()Z

    .line 16777602
    .line 16777603
    .line 16777604
    move-result v0

    .line 16777605
    if-ne v0, v2, :cond_188

    .line 16777606
    .line 16777607
    :cond_187
    return-void

    .line 16777608
    :cond_188
    invoke-interface {v3}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777609
    .line 16777610
    .line 16777611
    move-result-object v0

    .line 16777612
    if-eqz v0, :cond_1a5

    .line 16777613
    .line 16777614
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16777615
    .line 16777616
    .line 16777617
    move-result-object v1

    .line 16777618
    if-eqz v1, :cond_1a5

    .line 16777619
    .line 16777620
    const-class v0, Lcom/ss/android/ugc/tiktok/pns/feedsafety/api/algorefresh/abilities/IAlgoRefreshAbility;

    .line 16777621
    .line 16777622
    invoke-static {v1, v0, v8}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777623
    .line 16777624
    .line 16777625
    move-result-object v0

    .line 16777626
    check-cast v0, Lcom/ss/android/ugc/tiktok/pns/feedsafety/api/algorefresh/abilities/IAlgoRefreshAbility;

    .line 16777627
    .line 16777628
    if-eqz v0, :cond_1a5

    .line 16777629
    .line 16777630
    invoke-interface {v0}, Lcom/ss/android/ugc/tiktok/pns/feedsafety/api/algorefresh/abilities/IAlgoRefreshAbility;->wt()Z

    .line 16777631
    .line 16777632
    .line 16777633
    move-result v0

    .line 16777634
    if-ne v0, v2, :cond_1a5

    .line 16777635
    .line 16777636
    return-void

    .line 16777637
    :cond_1a5
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777638
    .line 16777639
    if-eqz v0, :cond_232

    .line 16777640
    .line 16777641
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->LJLJJLL()LX/08BV;

    .line 16777642
    .line 16777643
    .line 16777644
    move-result-object v1

    .line 16777645
    :goto_1ad
    instance-of v0, v1, Lcom/ss/android/ugc/aweme/feed/adapter/VideoBaseCell;

    .line 16777646
    .line 16777647
    if-eqz v0, :cond_187

    .line 16777648
    .line 16777649
    check-cast v1, Lcom/bytedance/ext_power_list/AssemReusedContainer;

    .line 16777650
    .line 16777651
    if-eqz v1, :cond_187

    .line 16777652
    .line 16777653
    invoke-static {v1}, LX/06GI;->LIZIZ(LX/02xX;)LX/03so;

    .line 16777654
    .line 16777655
    .line 16777656
    move-result-object v1

    .line 16777657
    const-class v0, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAbility;

    .line 16777658
    .line 16777659
    invoke-static {v1, v0, v8}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777660
    .line 16777661
    .line 16777662
    move-result-object v1

    .line 16777663
    check-cast v1, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAbility;

    .line 16777664
    .line 16777665
    if-eqz v1, :cond_187

    .line 16777666
    .line 16777667
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777668
    .line 16777669
    if-eqz v0, :cond_1cb

    .line 16777670
    .line 16777671
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777672
    .line 16777673
    .line 16777674
    move-result-object v8

    .line 16777675
    :cond_1cb
    iget-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16777676
    .line 16777677
    new-instance v3, LX/1Plu;

    .line 16777678
    .line 16777679
    invoke-direct {v3, v5, v1, v8, v0}, LX/1Plu;-><init>(Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAbility;Ljava/lang/String;Ljava/lang/String;)V

    .line 16777680
    .line 16777681
    .line 16777682
    iget-object v8, v3, LX/1Plu;->LJIIIIZZ:Landroid/view/View;

    .line 16777683
    .line 16777684
    if-eqz v8, :cond_1f9

    .line 16777685
    .line 16777686
    invoke-static {v7, v8}, LX/00kD;->LJLLILLLL(ILandroid/view/View;)V

    .line 16777687
    .line 16777688
    .line 16777689
    const/4 v0, 0x0

    .line 16777690
    invoke-static {v8, v0}, LX/00kD;->W4(Landroid/view/View;F)V

    .line 16777691
    .line 16777692
    .line 16777693
    const/4 v0, 0x2

    .line 16777694
    new-array v1, v0, [F

    .line 16777695
    .line 16777696
    fill-array-data v1, :array_23e

    .line 16777697
    .line 16777698
    .line 16777699
    const-string v0, "alpha"

    .line 16777700
    .line 16777701
    invoke-static {v8, v0, v1}, Landroid/animation/ObjectAnimator;->ofFloat(Ljava/lang/Object;Ljava/lang/String;[F)Landroid/animation/ObjectAnimator;

    .line 16777702
    .line 16777703
    .line 16777704
    move-result-object v8

    .line 16777705
    const-wide/16 v0, 0x12c

    .line 16777706
    .line 16777707
    invoke-virtual {v8, v0, v1}, Landroid/animation/ObjectAnimator;->setDuration(J)Landroid/animation/ObjectAnimator;

    .line 16777708
    .line 16777709
    .line 16777710
    new-instance v0, Landroid/view/animation/LinearInterpolator;

    .line 16777711
    .line 16777712
    invoke-direct {v0}, Landroid/view/animation/LinearInterpolator;-><init>()V

    .line 16777713
    .line 16777714
    .line 16777715
    invoke-virtual {v8, v0}, Landroid/animation/ValueAnimator;->setInterpolator(Landroid/animation/TimeInterpolator;)V

    .line 16777716
    .line 16777717
    .line 16777718
    invoke-static {v8}, LX/00kD;->ke(Landroid/animation/ObjectAnimator;)V

    .line 16777719
    .line 16777720
    .line 16777721
    :cond_1f9
    iput-boolean v2, v3, LX/1Plu;->LJIILLIIL:Z

    .line 16777722
    .line 16777723
    invoke-virtual {v3}, LX/1Plu;->LJII()V

    .line 16777724
    .line 16777725
    .line 16777726
    iget-object v1, v3, LX/1Plu;->LJI:LX/0Ar1;

    .line 16777727
    .line 16777728
    if-eqz v1, :cond_20a

    .line 16777729
    .line 16777730
    new-instance v0, LX/13CN;

    .line 16777731
    .line 16777732
    invoke-direct {v0, v1, v3}, LX/13CN;-><init>(LX/0Ar1;LX/1Plu;)V

    .line 16777733
    .line 16777734
    .line 16777735
    invoke-static {v1, v0}, Lcom/bytedance/bpea/transmit/hook/HandlerHook;->viewPost(Landroid/view/View;Ljava/lang/Runnable;)Z

    .line 16777736
    .line 16777737
    .line 16777738
    :cond_20a
    invoke-static {}, LX/042d;->LJI()Ljava/util/concurrent/ExecutorService;

    .line 16777739
    .line 16777740
    .line 16777741
    move-result-object v1

    .line 16777742
    new-instance v0, LX/0ZHm;

    .line 16777743
    .line 16777744
    invoke-direct {v0, v3, v2}, LX/0ZHm;-><init>(LX/1Plu;Z)V

    .line 16777745
    .line 16777746
    .line 16777747
    invoke-interface {v1, v0}, Ljava/util/concurrent/ExecutorService;->execute(Ljava/lang/Runnable;)V

    .line 16777748
    .line 16777749
    .line 16777750
    sget-object v2, LX/0Bwe;->LIZ:Lcom/bytedance/keva/Keva;

    .line 16777751
    .line 16777752
    invoke-virtual {v2, v6, v7}, Lcom/bytedance/keva/Keva;->getInt(Ljava/lang/String;I)I

    .line 16777753
    .line 16777754
    .line 16777755
    move-result v0

    .line 16777756
    add-int/lit8 v0, v0, 0x1

    .line 16777757
    .line 16777758
    invoke-virtual {v2, v6, v0}, Lcom/bytedance/keva/Keva;->storeInt(Ljava/lang/String;I)V

    .line 16777759
    .line 16777760
    .line 16777761
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 16777762
    .line 16777763
    .line 16777764
    move-result-wide v0

    .line 16777765
    invoke-virtual {v2, v4, v0, v1}, Lcom/bytedance/keva/Keva;->storeLong(Ljava/lang/String;J)V

    .line 16777766
    .line 16777767
    .line 16777768
    new-instance v0, LX/1Plv;

    .line 16777769
    .line 16777770
    invoke-direct {v0, v5}, LX/1Plv;-><init>(Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;)V

    .line 16777771
    .line 16777772
    .line 16777773
    iput-object v0, v3, LX/1Plu;->LJIILIIL:Lkotlin/jvm/functions/Function0;

    .line 16777774
    .line 16777775
    iput-object v3, v5, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->LLLFF:LX/1Plu;

    .line 16777776
    .line 16777777
    return-void

    .line 16777778
    :cond_232
    move-object v1, v8

    .line 16777779
    goto/16 :goto_1ad

    .line 16777780
    .line 16777781
    :cond_235
    const/4 v0, 0x0

    .line 16777782
    goto/16 :goto_13d

    .line 16777783
    .line 16777784
    :cond_238
    move-object v1, v8

    .line 16777785
    goto/16 :goto_12b

    .line 16777786
    .line 16777787
    :cond_23b
    const/4 v0, 0x0

    .line 16777788
    goto/16 :goto_b3

    .line 16777789
    .line 16777790
    :array_23e
    .array-data 4
        0x0
        0x3f800000    # 1.0f
    .end array-data
.end method

.method public onStopTrackingTouch(Landroid/widget/SeekBar;)V
    .registers 15

    .prologue
    .line 16777216
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtime()J

    .line 16777217
    .line 16777218
    .line 16777219
    move-result-wide v0

    .line 16777220
    iput-wide v0, p0, LX/08Vk;->LLLFZ:J

    .line 16777221
    .line 16777222
    const/4 v4, 0x0

    .line 16777223
    iput-boolean v4, p0, LX/08Vk;->LLLF:Z

    .line 16777224
    .line 16777225
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 16777226
    .line 16777227
    .line 16777228
    move-result-object v1

    .line 16777229
    const-class v0, Lcom/ss/android/ugc/aweme/feed/FypAutoScrollService;

    .line 16777230
    .line 16777231
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 16777232
    .line 16777233
    .line 16777234
    move-result-object v0

    .line 16777235
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/FypAutoScrollService;

    .line 16777236
    .line 16777237
    invoke-interface {v0, v4}, Lcom/ss/android/ugc/aweme/feed/FypAutoScrollService;->LJIIL(Z)V

    .line 16777238
    .line 16777239
    .line 16777240
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777241
    .line 16777242
    const/4 v5, 0x0

    .line 16777243
    if-eqz v0, :cond_3c

    .line 16777244
    .line 16777245
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777246
    .line 16777247
    .line 16777248
    move-result-object v0

    .line 16777249
    if-eqz v0, :cond_3c

    .line 16777250
    .line 16777251
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16777252
    .line 16777253
    .line 16777254
    move-result-object v1

    .line 16777255
    if-eqz v1, :cond_3c

    .line 16777256
    .line 16777257
    const-class v0, Lcom/ss/android/ugc/feed/platform/panel/midad/IMidAdAbility;

    .line 16777258
    .line 16777259
    invoke-static {v1, v0, v5}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777260
    .line 16777261
    .line 16777262
    move-result-object v0

    .line 16777263
    check-cast v0, Lcom/ss/android/ugc/feed/platform/panel/midad/IMidAdAbility;

    .line 16777264
    .line 16777265
    if-eqz v0, :cond_3c

    .line 16777266
    .line 16777267
    invoke-interface {v0}, Lcom/ss/android/ugc/feed/platform/panel/midad/IMidAdAbility;->Bv0()Lcom/ss/android/ugc/aweme/ad/feed/midad/IMidAdCellAbility;

    .line 16777268
    .line 16777269
    .line 16777270
    move-result-object v0

    .line 16777271
    if-eqz v0, :cond_3c

    .line 16777272
    .line 16777273
    invoke-interface {v0, v4}, Lcom/ss/android/ugc/aweme/ad/feed/midad/IMidAdCellAbility;->Ff1(Z)V

    .line 16777274
    .line 16777275
    .line 16777276
    :cond_3c
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777277
    .line 16777278
    if-eqz v0, :cond_a9

    .line 16777279
    .line 16777280
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->LJLJJLL()LX/08BV;

    .line 16777281
    .line 16777282
    .line 16777283
    move-result-object v0

    .line 16777284
    if-eqz v0, :cond_a9

    .line 16777285
    .line 16777286
    invoke-static {v0}, LX/0CB5;->LIZIZ(LX/08BV;)LX/03so;

    .line 16777287
    .line 16777288
    .line 16777289
    move-result-object v1

    .line 16777290
    if-eqz v1, :cond_a9

    .line 16777291
    .line 16777292
    const-class v0, Lcom/ss/android/ugc/aweme/feed/collection/collection/ability/UserFeedViewPagerAbility;

    .line 16777293
    .line 16777294
    invoke-static {v1, v0, v5}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777295
    .line 16777296
    .line 16777297
    move-result-object v0

    .line 16777298
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/collection/collection/ability/UserFeedViewPagerAbility;

    .line 16777299
    .line 16777300
    if-eqz v0, :cond_a9

    .line 16777301
    .line 16777302
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/collection/collection/ability/UserFeedViewPagerAbility;->kT1()LX/0BHY;

    .line 16777303
    .line 16777304
    .line 16777305
    move-result-object v2

    .line 16777306
    if-eqz v2, :cond_a9

    .line 16777307
    .line 16777308
    instance-of v0, v2, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777309
    .line 16777310
    if-eqz v0, :cond_77

    .line 16777311
    .line 16777312
    move-object v0, v2

    .line 16777313
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777314
    .line 16777315
    if-eqz v0, :cond_77

    .line 16777316
    .line 16777317
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;->rN2()LX/02xX;

    .line 16777318
    .line 16777319
    .line 16777320
    move-result-object v0

    .line 16777321
    if-eqz v0, :cond_77

    .line 16777322
    .line 16777323
    invoke-static {v0}, LX/06GI;->LIZIZ(LX/02xX;)LX/03so;

    .line 16777324
    .line 16777325
    .line 16777326
    move-result-object v1

    .line 16777327
    const-class v0, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;

    .line 16777328
    .line 16777329
    invoke-static {v1, v0, v4}, LX/06Cb;->LIZIZ(LX/03so;Ljava/lang/Class;Z)Ljava/util/List;

    .line 16777330
    .line 16777331
    .line 16777332
    move-result-object v0

    .line 16777333
    if-nez v0, :cond_95

    .line 16777334
    .line 16777335
    :cond_77
    invoke-interface {v2}, LX/08BV;->q1()LX/08BV;

    .line 16777336
    .line 16777337
    .line 16777338
    move-result-object v1

    .line 16777339
    instance-of v0, v1, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777340
    .line 16777341
    if-eqz v0, :cond_a9

    .line 16777342
    .line 16777343
    check-cast v1, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;

    .line 16777344
    .line 16777345
    if-eqz v1, :cond_a9

    .line 16777346
    .line 16777347
    invoke-interface {v1}, Lcom/ss/android/ugc/aweme/feed/collection/common/ability/CommonCellAbility;->rN2()LX/02xX;

    .line 16777348
    .line 16777349
    .line 16777350
    move-result-object v0

    .line 16777351
    if-eqz v0, :cond_a9

    .line 16777352
    .line 16777353
    invoke-static {v0}, LX/06GI;->LIZIZ(LX/02xX;)LX/03so;

    .line 16777354
    .line 16777355
    .line 16777356
    move-result-object v1

    .line 16777357
    const-class v0, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;

    .line 16777358
    .line 16777359
    invoke-static {v1, v0, v4}, LX/06Cb;->LIZIZ(LX/03so;Ljava/lang/Class;Z)Ljava/util/List;

    .line 16777360
    .line 16777361
    .line 16777362
    move-result-object v0

    .line 16777363
    if-eqz v0, :cond_a9

    .line 16777364
    .line 16777365
    :cond_95
    invoke-interface {v0}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    .line 16777366
    .line 16777367
    .line 16777368
    move-result-object v1

    .line 16777369
    :goto_99
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    .line 16777370
    .line 16777371
    .line 16777372
    move-result v0

    .line 16777373
    if-eqz v0, :cond_a9

    .line 16777374
    .line 16777375
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    .line 16777376
    .line 16777377
    .line 16777378
    move-result-object v0

    .line 16777379
    check-cast v0, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;

    .line 16777380
    .line 16777381
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/collection/common/protocol/CleanModeProtocol;->ZA0()V

    .line 16777382
    .line 16777383
    .line 16777384
    goto :goto_99

    .line 16777385
    :cond_a9
    new-instance v1, LX/09ei;

    .line 16777386
    .line 16777387
    const/4 v0, 0x4

    .line 16777388
    invoke-direct {v1, v4, v4, v0}, LX/09ei;-><init>(ZZI)V

    .line 16777389
    .line 16777390
    .line 16777391
    invoke-static {v1}, LX/096q;->LIZ(Lcom/ss/android/ugc/governance/eventbus/IEvent;)Lcom/ss/android/ugc/governance/eventbus/IEvent;

    .line 16777392
    .line 16777393
    .line 16777394
    invoke-static {}, LX/0BHV;->LIZ()Z

    .line 16777395
    .line 16777396
    .line 16777397
    move-result v0

    .line 16777398
    const/4 v3, 0x1

    .line 16777399
    if-eqz v0, :cond_e2

    .line 16777400
    .line 16777401
    instance-of v0, p1, LX/08Io;

    .line 16777402
    .line 16777403
    if-eqz v0, :cond_e2

    .line 16777404
    .line 16777405
    move-object v6, p1

    .line 16777406
    check-cast v6, LX/08Io;

    .line 16777407
    .line 16777408
    if-eqz v6, :cond_e2

    .line 16777409
    .line 16777410
    invoke-virtual {v6}, Landroid/widget/ProgressBar;->getMax()I

    .line 16777411
    .line 16777412
    .line 16777413
    move-result v0

    .line 16777414
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 16777415
    .line 16777416
    .line 16777417
    move-result-object v1

    .line 16777418
    invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I

    .line 16777419
    .line 16777420
    .line 16777421
    move-result v0

    .line 16777422
    if-lez v0, :cond_e2

    .line 16777423
    .line 16777424
    invoke-virtual {v1}, Ljava/lang/Integer;->intValue()I

    .line 16777425
    .line 16777426
    .line 16777427
    move-result v2

    .line 16777428
    invoke-virtual {v6}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16777429
    .line 16777430
    .line 16777431
    move-result v0

    .line 16777432
    int-to-float v1, v0

    .line 16777433
    int-to-float v0, v2

    .line 16777434
    div-float/2addr v1, v0

    .line 16777435
    const/16 v0, 0x64

    .line 16777436
    .line 16777437
    int-to-float v0, v0

    .line 16777438
    mul-float/2addr v1, v0

    .line 16777439
    invoke-virtual {v6, v1}, LX/08Io;->setLastProgress(F)V

    .line 16777440
    .line 16777441
    .line 16777442
    :cond_e2
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777443
    .line 16777444
    const-string v8, ""

    .line 16777445
    .line 16777446
    if-eqz v0, :cond_711

    .line 16777447
    .line 16777448
    invoke-interface {v0}, LX/05fO;->LJII()Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777449
    .line 16777450
    .line 16777451
    move-result-object v2

    .line 16777452
    if-eqz v2, :cond_711

    .line 16777453
    .line 16777454
    sget-object v1, LX/0C4C;->LIZ:LX/0C4C;

    .line 16777455
    .line 16777456
    invoke-virtual {v2}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777457
    .line 16777458
    .line 16777459
    move-result-object v0

    .line 16777460
    if-nez v0, :cond_f7

    .line 16777461
    .line 16777462
    move-object v0, v8

    .line 16777463
    :cond_f7
    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777464
    .line 16777465
    .line 16777466
    invoke-static {v0}, LX/0C4C;->LIZ(Ljava/lang/String;)Z

    .line 16777467
    .line 16777468
    .line 16777469
    move-result v0

    .line 16777470
    if-eqz v0, :cond_711

    .line 16777471
    .line 16777472
    invoke-virtual {v2}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777473
    .line 16777474
    .line 16777475
    move-result-object v1

    .line 16777476
    if-nez v1, :cond_107

    .line 16777477
    .line 16777478
    move-object v1, v8

    .line 16777479
    :cond_107
    sget-object v0, LX/0C4C;->LJIIL:Ljava/util/Set;

    .line 16777480
    .line 16777481
    check-cast v0, Ljava/util/HashSet;

    .line 16777482
    .line 16777483
    invoke-virtual {v0, v1}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    .line 16777484
    .line 16777485
    .line 16777486
    move-result v0

    .line 16777487
    if-nez v0, :cond_711

    .line 16777488
    .line 16777489
    const/4 v0, 0x1

    .line 16777490
    :goto_112
    if-eqz p1, :cond_70e

    .line 16777491
    .line 16777492
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16777493
    .line 16777494
    .line 16777495
    move-result v2

    .line 16777496
    :goto_118
    if-eqz v0, :cond_70b

    .line 16777497
    .line 16777498
    iget v0, p0, LX/08Vk;->LLLIZZ:I

    .line 16777499
    .line 16777500
    if-gt v0, v2, :cond_70b

    .line 16777501
    .line 16777502
    const/4 v10, 0x1

    .line 16777503
    :goto_11f
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777504
    .line 16777505
    if-eqz v0, :cond_708

    .line 16777506
    .line 16777507
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777508
    .line 16777509
    .line 16777510
    move-result-object v0

    .line 16777511
    if-eqz v0, :cond_708

    .line 16777512
    .line 16777513
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->Bx2()I

    .line 16777514
    .line 16777515
    .line 16777516
    move-result v0

    .line 16777517
    if-nez v0, :cond_708

    .line 16777518
    .line 16777519
    const/4 v9, 0x1

    .line 16777520
    :goto_130
    iget-object v6, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777521
    .line 16777522
    invoke-static {}, LX/02nu;->LIZ()Ljava/lang/StringBuilder;

    .line 16777523
    .line 16777524
    .line 16777525
    move-result-object v1

    .line 16777526
    const-string v0, "onStopTrackingTouch, hasTouchMove="

    .line 16777527
    .line 16777528
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777529
    .line 16777530
    .line 16777531
    iget-boolean v0, p0, LX/08Vk;->LLLFF:Z

    .line 16777532
    .line 16777533
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    .line 16777534
    .line 16777535
    .line 16777536
    const-string v0, ", isCurVideoPaused: "

    .line 16777537
    .line 16777538
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16777539
    .line 16777540
    .line 16777541
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 16777542
    .line 16777543
    .line 16777544
    move-result v0

    .line 16777545
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    .line 16777546
    .line 16777547
    .line 16777548
    invoke-static {v1}, LX/02nu;->LIZIZ(Ljava/lang/StringBuilder;)Ljava/lang/String;

    .line 16777549
    .line 16777550
    .line 16777551
    move-result-object v0

    .line 16777552
    invoke-virtual {v6, v0}, LX/05f6;->LJ(Ljava/lang/String;)V

    .line 16777553
    .line 16777554
    .line 16777555
    iget-boolean v0, p0, LX/08Vk;->LLLFF:Z

    .line 16777556
    .line 16777557
    if-nez v0, :cond_15d

    .line 16777558
    .line 16777559
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 16777560
    .line 16777561
    .line 16777562
    move-result v0

    .line 16777563
    if-eqz v0, :cond_1c4

    .line 16777564
    .line 16777565
    :cond_15d
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777566
    .line 16777567
    if-eqz v0, :cond_704

    .line 16777568
    .line 16777569
    invoke-interface {v0}, LX/05fO;->LJII()Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777570
    .line 16777571
    .line 16777572
    move-result-object v6

    .line 16777573
    if-eqz v6, :cond_705

    .line 16777574
    .line 16777575
    invoke-virtual {v6}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getContentModel()Lcom/ss/android/ugc/aweme/feed/model/ContentModel;

    .line 16777576
    .line 16777577
    .line 16777578
    move-result-object v0

    .line 16777579
    if-eqz v0, :cond_705

    .line 16777580
    .line 16777581
    iget-object v0, v0, Lcom/ss/android/ugc/aweme/feed/model/ContentModel;->standardBusinessModel:Lcom/ss/android/ugc/aweme/feed/model/StandardBusinessModel;

    .line 16777582
    .line 16777583
    if-eqz v0, :cond_705

    .line 16777584
    .line 16777585
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/StandardBusinessModel;->getSubOnlyVideo()Lcom/ss/android/ugc/aweme/feed/model/SubOnlyVideo;

    .line 16777586
    .line 16777587
    .line 16777588
    move-result-object v0

    .line 16777589
    if-eqz v0, :cond_705

    .line 16777590
    .line 16777591
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/SubOnlyVideo;->getShouldShowPaywall()Ljava/lang/Boolean;

    .line 16777592
    .line 16777593
    .line 16777594
    move-result-object v1

    .line 16777595
    sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    .line 16777596
    .line 16777597
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777598
    .line 16777599
    .line 16777600
    move-result v0

    .line 16777601
    :goto_181
    const/high16 v7, 0x42c80000    # 100.0f

    .line 16777602
    .line 16777603
    if-eqz v0, :cond_400

    .line 16777604
    .line 16777605
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 16777606
    .line 16777607
    .line 16777608
    move-result-object v1

    .line 16777609
    const-class v0, Lcom/ss/android/ugc/aweme/live/ILiveOuterService;

    .line 16777610
    .line 16777611
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 16777612
    .line 16777613
    .line 16777614
    move-result-object v0

    .line 16777615
    check-cast v0, Lcom/ss/android/ugc/aweme/live/ILiveOuterService;

    .line 16777616
    .line 16777617
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/live/ILiveOuterService;->A3()LX/05Hu;

    .line 16777618
    .line 16777619
    .line 16777620
    move-result-object v1

    .line 16777621
    if-eqz v6, :cond_3fd

    .line 16777622
    .line 16777623
    invoke-virtual {v6}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAuthor()Lcom/ss/android/ugc/aweme/profile/model/User;

    .line 16777624
    .line 16777625
    .line 16777626
    move-result-object v0

    .line 16777627
    :goto_19b
    invoke-interface {v1, v0}, LX/05Hu;->LJJIII(Lcom/ss/android/ugc/aweme/profile/model/User;)Z

    .line 16777628
    .line 16777629
    .line 16777630
    move-result v0

    .line 16777631
    if-nez v0, :cond_400

    .line 16777632
    .line 16777633
    if-eqz v9, :cond_400

    .line 16777634
    .line 16777635
    invoke-static {v6}, LX/0BHU;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/Float;

    .line 16777636
    .line 16777637
    .line 16777638
    move-result-object v0

    .line 16777639
    if-eqz v0, :cond_3ea

    .line 16777640
    .line 16777641
    invoke-virtual {v0}, Ljava/lang/Float;->floatValue()F

    .line 16777642
    .line 16777643
    .line 16777644
    move-result v1

    .line 16777645
    int-to-float v0, v2

    .line 16777646
    mul-float/2addr v1, v7

    .line 16777647
    mul-float/2addr v1, v7

    .line 16777648
    cmpl-float v0, v0, v1

    .line 16777649
    .line 16777650
    if-lez v0, :cond_3ea

    .line 16777651
    .line 16777652
    :goto_1b4
    if-eqz p1, :cond_1c4

    .line 16777653
    .line 16777654
    new-instance v1, LX/07bu;

    .line 16777655
    .line 16777656
    invoke-direct {v1, p1}, LX/07bu;-><init>(Landroid/view/View;)V

    .line 16777657
    .line 16777658
    .line 16777659
    const v0, 0x7f117a70

    .line 16777660
    .line 16777661
    .line 16777662
    invoke-virtual {v1, v0}, LX/07bu;->LJIIJ(I)V

    .line 16777663
    .line 16777664
    .line 16777665
    invoke-virtual {v1}, LX/07bu;->LJIIL()V

    .line 16777666
    .line 16777667
    .line 16777668
    :cond_1c4
    :goto_1c4
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 16777669
    .line 16777670
    .line 16777671
    move-result v0

    .line 16777672
    if-eqz v0, :cond_1d5

    .line 16777673
    .line 16777674
    iget-object v1, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777675
    .line 16777676
    if-eqz v1, :cond_1d5

    .line 16777677
    .line 16777678
    invoke-interface {v1}, LX/05fO;->LJII()Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777679
    .line 16777680
    .line 16777681
    move-result-object v0

    .line 16777682
    invoke-interface {v1, v0}, LX/05fP;->LJJIIJZLJL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)V

    .line 16777683
    .line 16777684
    .line 16777685
    :cond_1d5
    invoke-static {}, LX/08Xg;->LIZ()Z

    .line 16777686
    .line 16777687
    .line 16777688
    move-result v0

    .line 16777689
    if-eqz v0, :cond_3e6

    .line 16777690
    .line 16777691
    const-wide/16 v0, 0x12c

    .line 16777692
    .line 16777693
    :goto_1dd
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 16777694
    .line 16777695
    .line 16777696
    move-result-object v5

    .line 16777697
    invoke-virtual {v5}, LX/08WY;->LIZIZ()V

    .line 16777698
    .line 16777699
    .line 16777700
    iget-object v5, p0, LX/08Vk;->LLJJIJIIJIL:Landroid/view/ViewGroup;

    .line 16777701
    .line 16777702
    const/high16 v7, 0x3f800000    # 1.0f

    .line 16777703
    .line 16777704
    invoke-static {v7, v5}, LX/00kD;->U4(FLandroid/view/ViewGroup;)V

    .line 16777705
    .line 16777706
    .line 16777707
    iget-object v5, p0, LX/08Vk;->LLJJIJIIJIL:Landroid/view/ViewGroup;

    .line 16777708
    .line 16777709
    invoke-virtual {v5}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 16777710
    .line 16777711
    .line 16777712
    move-result-object v5

    .line 16777713
    const/4 v6, 0x0

    .line 16777714
    if-eqz v5, :cond_203

    .line 16777715
    .line 16777716
    invoke-virtual {v5, v6}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 16777717
    .line 16777718
    .line 16777719
    move-result-object v5

    .line 16777720
    if-eqz v5, :cond_203

    .line 16777721
    .line 16777722
    invoke-virtual {v5, v0, v1}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 16777723
    .line 16777724
    .line 16777725
    move-result-object v5

    .line 16777726
    if-eqz v5, :cond_203

    .line 16777727
    .line 16777728
    invoke-virtual {v5}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 16777729
    .line 16777730
    .line 16777731
    :cond_203
    iget-object v5, p0, LX/08Vk;->LLJJJ:Landroid/widget/ImageView;

    .line 16777732
    .line 16777733
    invoke-static {v5, v7}, LX/00kD;->Y4(Landroid/widget/ImageView;F)V

    .line 16777734
    .line 16777735
    .line 16777736
    iget-object v5, p0, LX/08Vk;->LLJJJ:Landroid/widget/ImageView;

    .line 16777737
    .line 16777738
    invoke-virtual {v5}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 16777739
    .line 16777740
    .line 16777741
    move-result-object v5

    .line 16777742
    if-eqz v5, :cond_21f

    .line 16777743
    .line 16777744
    invoke-virtual {v5, v6}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 16777745
    .line 16777746
    .line 16777747
    move-result-object v5

    .line 16777748
    if-eqz v5, :cond_21f

    .line 16777749
    .line 16777750
    invoke-virtual {v5, v0, v1}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 16777751
    .line 16777752
    .line 16777753
    move-result-object v5

    .line 16777754
    if-eqz v5, :cond_21f

    .line 16777755
    .line 16777756
    invoke-virtual {v5}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 16777757
    .line 16777758
    .line 16777759
    :cond_21f
    iget-object v5, p0, LX/08Vk;->LLJJIJIL:Landroid/view/View;

    .line 16777760
    .line 16777761
    invoke-static {v5, v7}, LX/00kD;->W4(Landroid/view/View;F)V

    .line 16777762
    .line 16777763
    .line 16777764
    iget-object v5, p0, LX/08Vk;->LLJJIJIL:Landroid/view/View;

    .line 16777765
    .line 16777766
    invoke-virtual {v5}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 16777767
    .line 16777768
    .line 16777769
    move-result-object v5

    .line 16777770
    if-eqz v5, :cond_23b

    .line 16777771
    .line 16777772
    invoke-virtual {v5, v6}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 16777773
    .line 16777774
    .line 16777775
    move-result-object v5

    .line 16777776
    if-eqz v5, :cond_23b

    .line 16777777
    .line 16777778
    invoke-virtual {v5, v0, v1}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 16777779
    .line 16777780
    .line 16777781
    move-result-object v5

    .line 16777782
    if-eqz v5, :cond_23b

    .line 16777783
    .line 16777784
    invoke-virtual {v5}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 16777785
    .line 16777786
    .line 16777787
    :cond_23b
    iget-object v5, p0, LX/08Vk;->LLJJJJJIL:LX/08WH;

    .line 16777788
    .line 16777789
    if-eqz v5, :cond_25a

    .line 16777790
    .line 16777791
    invoke-static {v5}, LX/06w5;->LIZJ(Ljava/lang/Object;)V

    .line 16777792
    .line 16777793
    .line 16777794
    invoke-virtual {v5, v7}, Landroid/view/View;->setAlpha(F)V

    .line 16777795
    .line 16777796
    .line 16777797
    invoke-virtual {v5}, Landroid/view/View;->animate()Landroid/view/ViewPropertyAnimator;

    .line 16777798
    .line 16777799
    .line 16777800
    move-result-object v5

    .line 16777801
    if-eqz v5, :cond_25a

    .line 16777802
    .line 16777803
    invoke-virtual {v5, v6}, Landroid/view/ViewPropertyAnimator;->alpha(F)Landroid/view/ViewPropertyAnimator;

    .line 16777804
    .line 16777805
    .line 16777806
    move-result-object v5

    .line 16777807
    if-eqz v5, :cond_25a

    .line 16777808
    .line 16777809
    invoke-virtual {v5, v0, v1}, Landroid/view/ViewPropertyAnimator;->setDuration(J)Landroid/view/ViewPropertyAnimator;

    .line 16777810
    .line 16777811
    .line 16777812
    move-result-object v0

    .line 16777813
    if-eqz v0, :cond_25a

    .line 16777814
    .line 16777815
    invoke-virtual {v0}, Landroid/view/ViewPropertyAnimator;->start()V

    .line 16777816
    .line 16777817
    .line 16777818
    :cond_25a
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777819
    .line 16777820
    if-eqz v0, :cond_261

    .line 16777821
    .line 16777822
    invoke-virtual {v0, v3}, LX/08Ic;->setSeekBarShowType(I)V

    .line 16777823
    .line 16777824
    .line 16777825
    :cond_261
    iget-object v1, p0, LX/08Vk;->LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 16777826
    .line 16777827
    iget-object v0, p0, LX/08Vk;->LLLLILI:LX/08Xl;

    .line 16777828
    .line 16777829
    invoke-virtual {v1, v0}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 16777830
    .line 16777831
    .line 16777832
    iput-boolean v3, p0, LX/08Vk;->LLLIL:Z

    .line 16777833
    .line 16777834
    iget-object v6, p0, LX/08Vk;->LLJLILLLLZIIL:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;

    .line 16777835
    .line 16777836
    iget-object v5, p0, LX/08Vk;->LLLLILI:LX/08Xl;

    .line 16777837
    .line 16777838
    iget v0, p0, LX/08Vk;->LLL:I

    .line 16777839
    .line 16777840
    const/16 v7, 0x1e

    .line 16777841
    .line 16777842
    if-le v0, v7, :cond_3e2

    .line 16777843
    .line 16777844
    const-wide/16 v0, 0xbb8

    .line 16777845
    .line 16777846
    :goto_276
    invoke-static {v0, v1, v6, v5}, LX/00kD;->LJL(JLandroid/os/Handler;Ljava/lang/Runnable;)Z

    .line 16777847
    .line 16777848
    .line 16777849
    if-eqz p1, :cond_2a9

    .line 16777850
    .line 16777851
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777852
    .line 16777853
    if-eqz v0, :cond_2a9

    .line 16777854
    .line 16777855
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16777856
    .line 16777857
    .line 16777858
    move-result-object v0

    .line 16777859
    if-eqz v0, :cond_2a9

    .line 16777860
    .line 16777861
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16777862
    .line 16777863
    .line 16777864
    move-result-object v5

    .line 16777865
    if-eqz v5, :cond_2a9

    .line 16777866
    .line 16777867
    const-class v1, Lcom/ss/android/ugc/aweme/casting/api/ICastingAbility;

    .line 16777868
    .line 16777869
    const/4 v0, 0x0

    .line 16777870
    invoke-static {v5, v1, v0}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16777871
    .line 16777872
    .line 16777873
    move-result-object v6

    .line 16777874
    check-cast v6, Lcom/ss/android/ugc/aweme/casting/api/ICastingAbility;

    .line 16777875
    .line 16777876
    if-eqz v6, :cond_2a9

    .line 16777877
    .line 16777878
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16777879
    .line 16777880
    .line 16777881
    move-result v5

    .line 16777882
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getMax()I

    .line 16777883
    .line 16777884
    .line 16777885
    move-result v1

    .line 16777886
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777887
    .line 16777888
    if-eqz v0, :cond_3df

    .line 16777889
    .line 16777890
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777891
    .line 16777892
    .line 16777893
    move-result-object v0

    .line 16777894
    :goto_2a6
    invoke-interface {v6, v5, v1, v0}, Lcom/ss/android/ugc/aweme/casting/api/ICastingAbility;->nX0(IILjava/lang/String;)V

    .line 16777895
    .line 16777896
    .line 16777897
    :cond_2a9
    iget-object v6, p0, LX/08Vk;->LLJJJJ:Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;

    .line 16777898
    .line 16777899
    if-eqz v6, :cond_33b

    .line 16777900
    .line 16777901
    iget-object v8, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777902
    .line 16777903
    if-eqz p1, :cond_3dc

    .line 16777904
    .line 16777905
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getMax()I

    .line 16777906
    .line 16777907
    .line 16777908
    move-result v9

    .line 16777909
    :goto_2b5
    iget-object v5, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16777910
    .line 16777911
    if-eqz v8, :cond_32d

    .line 16777912
    .line 16777913
    invoke-static {}, LX/0AAT;->LIZ()Z

    .line 16777914
    .line 16777915
    .line 16777916
    move-result v0

    .line 16777917
    if-eqz v0, :cond_32d

    .line 16777918
    .line 16777919
    invoke-static {}, LX/0Bwf;->LIZ()I

    .line 16777920
    .line 16777921
    .line 16777922
    move-result v1

    .line 16777923
    const/4 v0, 0x2

    .line 16777924
    if-eq v1, v0, :cond_2cd

    .line 16777925
    .line 16777926
    invoke-static {}, LX/0Bwf;->LIZ()I

    .line 16777927
    .line 16777928
    .line 16777929
    move-result v1

    .line 16777930
    const/4 v0, 0x3

    .line 16777931
    if-ne v1, v0, :cond_32d

    .line 16777932
    .line 16777933
    :cond_2cd
    sget-object v0, LX/0A4m;->LIZ:LX/0A4m;

    .line 16777934
    .line 16777935
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    .line 16777936
    .line 16777937
    .line 16777938
    invoke-static {v8}, LX/0A4m;->LIZ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777939
    .line 16777940
    .line 16777941
    move-result v0

    .line 16777942
    if-eqz v0, :cond_32d

    .line 16777943
    .line 16777944
    if-eqz v5, :cond_3d9

    .line 16777945
    .line 16777946
    invoke-interface {v5}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->getEventType()Ljava/lang/String;

    .line 16777947
    .line 16777948
    .line 16777949
    move-result-object v1

    .line 16777950
    :goto_2de
    const-string v0, "homepage_hot"

    .line 16777951
    .line 16777952
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777953
    .line 16777954
    .line 16777955
    move-result v0

    .line 16777956
    if-eqz v0, :cond_32d

    .line 16777957
    .line 16777958
    invoke-virtual {v8}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777959
    .line 16777960
    .line 16777961
    move-result-object v0

    .line 16777962
    if-eqz v0, :cond_3d6

    .line 16777963
    .line 16777964
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777965
    .line 16777966
    .line 16777967
    move-result v0

    .line 16777968
    :goto_2f0
    invoke-static {v0}, LX/0BX3;->LIZJ(I)I

    .line 16777969
    .line 16777970
    .line 16777971
    move-result v0

    .line 16777972
    if-lt v0, v7, :cond_32d

    .line 16777973
    .line 16777974
    int-to-float v7, v9

    .line 16777975
    int-to-float v0, v2

    .line 16777976
    sub-float v1, v7, v0

    .line 16777977
    .line 16777978
    div-float/2addr v1, v7

    .line 16777979
    invoke-virtual {v8}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16777980
    .line 16777981
    .line 16777982
    move-result-object v0

    .line 16777983
    if-eqz v0, :cond_3d3

    .line 16777984
    .line 16777985
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16777986
    .line 16777987
    .line 16777988
    move-result v0

    .line 16777989
    :goto_305
    int-to-float v0, v0

    .line 16777990
    mul-float/2addr v1, v0

    .line 16777991
    const v0, 0x461c4000    # 10000.0f

    .line 16777992
    .line 16777993
    .line 16777994
    cmpg-float v0, v1, v0

    .line 16777995
    .line 16777996
    if-lez v0, :cond_32d

    .line 16777997
    .line 16777998
    if-eqz v5, :cond_35a

    .line 16777999
    .line 16778000
    invoke-interface {v5}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16778001
    .line 16778002
    .line 16778003
    move-result-object v0

    .line 16778004
    if-eqz v0, :cond_33c

    .line 16778005
    .line 16778006
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16778007
    .line 16778008
    .line 16778009
    move-result-object v2

    .line 16778010
    if-eqz v2, :cond_33c

    .line 16778011
    .line 16778012
    const-class v1, Lcom/ss/android/ugc/aweme/base/INonPersonalizedAbility;

    .line 16778013
    .line 16778014
    const/4 v0, 0x0

    .line 16778015
    invoke-static {v2, v1, v0}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16778016
    .line 16778017
    .line 16778018
    move-result-object v0

    .line 16778019
    check-cast v0, Lcom/ss/android/ugc/aweme/base/INonPersonalizedAbility;

    .line 16778020
    .line 16778021
    if-eqz v0, :cond_33c

    .line 16778022
    .line 16778023
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/base/INonPersonalizedAbility;->wt()Z

    .line 16778024
    .line 16778025
    .line 16778026
    move-result v0

    .line 16778027
    if-ne v0, v3, :cond_33c

    .line 16778028
    .line 16778029
    :cond_32d
    :goto_32d
    iget-object v1, v6, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->LLLFF:LX/1Plu;

    .line 16778030
    .line 16778031
    if-eqz v1, :cond_33b

    .line 16778032
    .line 16778033
    iget-boolean v0, v1, LX/1Plu;->LJIILLIIL:Z

    .line 16778034
    .line 16778035
    if-ne v0, v3, :cond_33b

    .line 16778036
    .line 16778037
    invoke-virtual {v1}, LX/1Plu;->LJ()V

    .line 16778038
    .line 16778039
    .line 16778040
    const/4 v0, 0x0

    .line 16778041
    iput-object v0, v6, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->LLLFF:LX/1Plu;

    .line 16778042
    .line 16778043
    :cond_33b
    return-void

    .line 16778044
    :cond_33c
    invoke-interface {v5}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->Ww2()Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;

    .line 16778045
    .line 16778046
    .line 16778047
    move-result-object v0

    .line 16778048
    if-eqz v0, :cond_35a

    .line 16778049
    .line 16778050
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/assem/ability/IFeedPanelPlatformAbility;->LJLILLLLZI()LX/03so;

    .line 16778051
    .line 16778052
    .line 16778053
    move-result-object v2

    .line 16778054
    if-eqz v2, :cond_35a

    .line 16778055
    .line 16778056
    const-class v1, Lcom/ss/android/ugc/tiktok/pns/feedsafety/api/algorefresh/abilities/IAlgoRefreshAbility;

    .line 16778057
    .line 16778058
    const/4 v0, 0x0

    .line 16778059
    invoke-static {v2, v1, v0}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16778060
    .line 16778061
    .line 16778062
    move-result-object v0

    .line 16778063
    check-cast v0, Lcom/ss/android/ugc/tiktok/pns/feedsafety/api/algorefresh/abilities/IAlgoRefreshAbility;

    .line 16778064
    .line 16778065
    if-eqz v0, :cond_35a

    .line 16778066
    .line 16778067
    invoke-interface {v0}, Lcom/ss/android/ugc/tiktok/pns/feedsafety/api/algorefresh/abilities/IAlgoRefreshAbility;->wt()Z

    .line 16778068
    .line 16778069
    .line 16778070
    move-result v0

    .line 16778071
    if-ne v0, v3, :cond_35a

    .line 16778072
    .line 16778073
    goto :goto_32d

    .line 16778074
    :cond_35a
    sget-object v0, LX/0Bwe;->LIZ:Lcom/bytedance/keva/Keva;

    .line 16778075
    .line 16778076
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    .line 16778077
    .line 16778078
    .line 16778079
    move-result-wide v8

    .line 16778080
    sget-object v7, LX/0Bwe;->LIZ:Lcom/bytedance/keva/Keva;

    .line 16778081
    .line 16778082
    const-string v2, "new_user_guide_shown_count"

    .line 16778083
    .line 16778084
    invoke-virtual {v7, v2, v4}, Lcom/bytedance/keva/Keva;->getInt(Ljava/lang/String;I)I

    .line 16778085
    .line 16778086
    .line 16778087
    move-result v1

    .line 16778088
    const/4 v0, 0x4

    .line 16778089
    if-ge v1, v0, :cond_32d

    .line 16778090
    .line 16778091
    const-string/jumbo v0, "ripple_shown_count"

    .line 16778092
    .line 16778093
    .line 16778094
    invoke-virtual {v7, v0, v4}, Lcom/bytedance/keva/Keva;->getInt(Ljava/lang/String;I)I

    .line 16778095
    .line 16778096
    .line 16778097
    move-result v0

    .line 16778098
    if-gtz v0, :cond_32d

    .line 16778099
    .line 16778100
    invoke-virtual {v7, v2, v4}, Lcom/bytedance/keva/Keva;->getInt(Ljava/lang/String;I)I

    .line 16778101
    .line 16778102
    .line 16778103
    move-result v0

    .line 16778104
    const-wide/16 v1, 0x0

    .line 16778105
    .line 16778106
    if-eqz v0, :cond_3d0

    .line 16778107
    .line 16778108
    if-eq v0, v3, :cond_3cc

    .line 16778109
    .line 16778110
    const-wide/32 v4, 0x240c8400

    .line 16778111
    .line 16778112
    .line 16778113
    :goto_381
    const-string v0, "last_new_user_guide_shown_time"

    .line 16778114
    .line 16778115
    invoke-virtual {v7, v0, v1, v2}, Lcom/bytedance/keva/Keva;->getLong(Ljava/lang/String;J)J

    .line 16778116
    .line 16778117
    .line 16778118
    move-result-wide v0

    .line 16778119
    sub-long/2addr v8, v0

    .line 16778120
    cmp-long v0, v8, v4

    .line 16778121
    .line 16778122
    if-lez v0, :cond_32d

    .line 16778123
    .line 16778124
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16778125
    .line 16778126
    if-eqz v0, :cond_3ca

    .line 16778127
    .line 16778128
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;->LJLJJLL()LX/08BV;

    .line 16778129
    .line 16778130
    .line 16778131
    move-result-object v1

    .line 16778132
    :goto_394
    instance-of v0, v1, Lcom/ss/android/ugc/aweme/feed/adapter/VideoBaseCell;

    .line 16778133
    .line 16778134
    if-eqz v0, :cond_33b

    .line 16778135
    .line 16778136
    check-cast v1, Lcom/bytedance/ext_power_list/AssemReusedContainer;

    .line 16778137
    .line 16778138
    if-eqz v1, :cond_33b

    .line 16778139
    .line 16778140
    invoke-static {v1}, LX/06GI;->LIZIZ(LX/02xX;)LX/03so;

    .line 16778141
    .line 16778142
    .line 16778143
    move-result-object v2

    .line 16778144
    const-class v1, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAbility;

    .line 16778145
    .line 16778146
    const/4 v0, 0x0

    .line 16778147
    invoke-static {v2, v1, v0}, LX/069H;->LIZ(LX/03so;Ljava/lang/Class;Ljava/lang/String;)LX/03Gb;

    .line 16778148
    .line 16778149
    .line 16778150
    move-result-object v5

    .line 16778151
    check-cast v5, Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAbility;

    .line 16778152
    .line 16778153
    if-eqz v5, :cond_33b

    .line 16778154
    .line 16778155
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778156
    .line 16778157
    if-eqz v0, :cond_3c8

    .line 16778158
    .line 16778159
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16778160
    .line 16778161
    .line 16778162
    move-result-object v4

    .line 16778163
    :goto_3b3
    iget-object v2, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16778164
    .line 16778165
    iget-object v1, v6, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->LLLFF:LX/1Plu;

    .line 16778166
    .line 16778167
    if-eqz v1, :cond_714

    .line 16778168
    .line 16778169
    iget-boolean v0, v1, LX/1Plu;->LJIILLIIL:Z

    .line 16778170
    .line 16778171
    if-ne v0, v3, :cond_714

    .line 16778172
    .line 16778173
    invoke-virtual {v1}, LX/1Plu;->LJI()V

    .line 16778174
    .line 16778175
    .line 16778176
    new-instance v0, LX/1Plw;

    .line 16778177
    .line 16778178
    invoke-direct {v0, v6}, LX/1Plw;-><init>(Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;)V

    .line 16778179
    .line 16778180
    .line 16778181
    iput-object v0, v1, LX/1Plu;->LJIILIIL:Lkotlin/jvm/functions/Function0;

    .line 16778182
    .line 16778183
    return-void

    .line 16778184
    :cond_3c8
    const/4 v4, 0x0

    .line 16778185
    goto :goto_3b3

    .line 16778186
    :cond_3ca
    const/4 v1, 0x0

    .line 16778187
    goto :goto_394

    .line 16778188
    :cond_3cc
    const-wide/32 v4, 0xf731400

    .line 16778189
    .line 16778190
    .line 16778191
    goto :goto_381

    .line 16778192
    :cond_3d0
    const-wide/16 v4, 0x0

    .line 16778193
    .line 16778194
    goto :goto_381

    .line 16778195
    :cond_3d3
    const/4 v0, 0x0

    .line 16778196
    goto/16 :goto_305

    .line 16778197
    .line 16778198
    :cond_3d6
    const/4 v0, 0x0

    .line 16778199
    goto/16 :goto_2f0

    .line 16778200
    .line 16778201
    :cond_3d9
    const/4 v1, 0x0

    .line 16778202
    goto/16 :goto_2de

    .line 16778203
    .line 16778204
    :cond_3dc
    const/4 v9, -0x1

    .line 16778205
    goto/16 :goto_2b5

    .line 16778206
    .line 16778207
    :cond_3df
    const/4 v0, 0x0

    .line 16778208
    goto/16 :goto_2a6

    .line 16778209
    .line 16778210
    :cond_3e2
    const-wide/16 v0, 0x3e8

    .line 16778211
    .line 16778212
    goto/16 :goto_276

    .line 16778213
    .line 16778214
    :cond_3e6
    const-wide/16 v0, 0x64

    .line 16778215
    .line 16778216
    goto/16 :goto_1dd

    .line 16778217
    .line 16778218
    :cond_3ea
    invoke-static {v6}, LX/0BHU;->LIZJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/Float;

    .line 16778219
    .line 16778220
    .line 16778221
    move-result-object v0

    .line 16778222
    if-eqz v0, :cond_400

    .line 16778223
    .line 16778224
    invoke-virtual {v0}, Ljava/lang/Float;->floatValue()F

    .line 16778225
    .line 16778226
    .line 16778227
    move-result v1

    .line 16778228
    int-to-float v0, v2

    .line 16778229
    mul-float/2addr v1, v7

    .line 16778230
    mul-float/2addr v1, v7

    .line 16778231
    cmpg-float v0, v0, v1

    .line 16778232
    .line 16778233
    if-gez v0, :cond_400

    .line 16778234
    .line 16778235
    goto/16 :goto_1b4

    .line 16778236
    .line 16778237
    :cond_3fd
    move-object v0, v5

    .line 16778238
    goto/16 :goto_19b

    .line 16778239
    .line 16778240
    :cond_400
    if-eqz v10, :cond_414

    .line 16778241
    .line 16778242
    if-eqz p1, :cond_1c4

    .line 16778243
    .line 16778244
    new-instance v1, LX/07bu;

    .line 16778245
    .line 16778246
    invoke-direct {v1, p1}, LX/07bu;-><init>(Landroid/view/View;)V

    .line 16778247
    .line 16778248
    .line 16778249
    const v0, 0x7f114575

    .line 16778250
    .line 16778251
    .line 16778252
    invoke-virtual {v1, v0}, LX/07bu;->LJIIJ(I)V

    .line 16778253
    .line 16778254
    .line 16778255
    invoke-virtual {v1}, LX/07bu;->LJIIL()V

    .line 16778256
    .line 16778257
    .line 16778258
    goto/16 :goto_1c4

    .line 16778259
    .line 16778260
    :cond_414
    iput-boolean v4, p0, LX/08Vk;->LLLFF:Z

    .line 16778261
    .line 16778262
    iget-object v6, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16778263
    .line 16778264
    invoke-static {}, LX/02nu;->LIZ()Ljava/lang/StringBuilder;

    .line 16778265
    .line 16778266
    .line 16778267
    move-result-object v1

    .line 16778268
    const-string v0, "progressbar seek, progress:"

    .line 16778269
    .line 16778270
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 16778271
    .line 16778272
    .line 16778273
    if-eqz p1, :cond_44b

    .line 16778274
    .line 16778275
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16778276
    .line 16778277
    .line 16778278
    move-result v0

    .line 16778279
    invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    .line 16778280
    .line 16778281
    .line 16778282
    move-result-object v0

    .line 16778283
    :goto_42b
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    .line 16778284
    .line 16778285
    .line 16778286
    invoke-static {v1}, LX/02nu;->LIZIZ(Ljava/lang/StringBuilder;)Ljava/lang/String;

    .line 16778287
    .line 16778288
    .line 16778289
    move-result-object v0

    .line 16778290
    invoke-virtual {v6, v0}, LX/05f6;->LJ(Ljava/lang/String;)V

    .line 16778291
    .line 16778292
    .line 16778293
    if-eqz p1, :cond_1c4

    .line 16778294
    .line 16778295
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16778296
    .line 16778297
    .line 16778298
    move-result v6

    .line 16778299
    iget-boolean v0, p0, LX/08Vk;->LLLILZ:Z

    .line 16778300
    .line 16778301
    if-nez v0, :cond_44d

    .line 16778302
    .line 16778303
    iput-boolean v3, p0, LX/08Vk;->LLLILZ:Z

    .line 16778304
    .line 16778305
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16778306
    .line 16778307
    const-string/jumbo v0, "seek not allowed! dispatch event not finished."

    .line 16778308
    .line 16778309
    .line 16778310
    invoke-virtual {v1, v0}, LX/05f6;->LJIILLIIL(Ljava/lang/String;)V

    .line 16778311
    .line 16778312
    .line 16778313
    goto/16 :goto_1c4

    .line 16778314
    .line 16778315
    :cond_44b
    move-object v0, v5

    .line 16778316
    goto :goto_42b

    .line 16778317
    :cond_44d
    invoke-static {}, LX/03Yp;->LJIILIIL()LX/03Yo;

    .line 16778318
    .line 16778319
    .line 16778320
    move-result-object v1

    .line 16778321
    int-to-float v0, v6

    .line 16778322
    div-float/2addr v0, v7

    .line 16778323
    invoke-interface {v1, v0}, LX/03Yo;->seek(F)V

    .line 16778324
    .line 16778325
    .line 16778326
    iget v0, p0, LX/08Vk;->LLLJL:I

    .line 16778327
    .line 16778328
    add-int/lit8 v0, v0, 0x1

    .line 16778329
    .line 16778330
    iput v0, p0, LX/08Vk;->LLLJL:I

    .line 16778331
    .line 16778332
    iget v0, p0, LX/08Vk;->LLLIIIL:I

    .line 16778333
    .line 16778334
    if-ge v0, v6, :cond_700

    .line 16778335
    .line 16778336
    const-string v12, "back"

    .line 16778337
    .line 16778338
    :goto_462
    if-ge v0, v6, :cond_6fb

    .line 16778339
    .line 16778340
    const-string/jumbo v10, "swipe_forward"

    .line 16778341
    .line 16778342
    .line 16778343
    :goto_467
    invoke-virtual {p0}, LX/08Vk;->LJIILIIL()Z

    .line 16778344
    .line 16778345
    .line 16778346
    move-result v11

    .line 16778347
    iget-object v0, p0, LX/08Vk;->LLJZIJLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778348
    .line 16778349
    if-eqz v0, :cond_6f8

    .line 16778350
    .line 16778351
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;

    .line 16778352
    .line 16778353
    .line 16778354
    move-result-object v0

    .line 16778355
    if-eqz v0, :cond_6f8

    .line 16778356
    .line 16778357
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Video;->getDuration()I

    .line 16778358
    .line 16778359
    .line 16778360
    move-result v7

    .line 16778361
    :goto_479
    sget-object v6, LX/0450;->LIZIZ:LX/0450;

    .line 16778362
    .line 16778363
    new-instance v9, LX/05oI;

    .line 16778364
    .line 16778365
    invoke-direct {v9}, LX/05oI;-><init>()V

    .line 16778366
    .line 16778367
    .line 16778368
    iget-object v1, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16778369
    .line 16778370
    const-string v0, "enter_from"

    .line 16778371
    .line 16778372
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778373
    .line 16778374
    .line 16778375
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778376
    .line 16778377
    invoke-static {v0}, LX/0AGB;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 16778378
    .line 16778379
    .line 16778380
    move-result-object v1

    .line 16778381
    const-string v0, "impr_type"

    .line 16778382
    .line 16778383
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778384
    .line 16778385
    .line 16778386
    const-string v0, "action_type"

    .line 16778387
    .line 16778388
    invoke-virtual {v9, v0, v12}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778389
    .line 16778390
    .line 16778391
    const-string v0, "is_pause"

    .line 16778392
    .line 16778393
    invoke-virtual {v9, v11, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778394
    .line 16778395
    .line 16778396
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778397
    .line 16778398
    invoke-static {v0}, LX/0AGB;->LIZLLL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 16778399
    .line 16778400
    .line 16778401
    move-result-object v1

    .line 16778402
    const-string v0, "author_id"

    .line 16778403
    .line 16778404
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778405
    .line 16778406
    .line 16778407
    const-string v1, "page_type"

    .line 16778408
    .line 16778409
    iget v0, p0, LX/08Vk;->LLLIIIIL:I

    .line 16778410
    .line 16778411
    invoke-virtual {v9, v0, v1}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778412
    .line 16778413
    .line 16778414
    sget-object v1, LX/07QB;->LIZ:LX/07QD;

    .line 16778415
    .line 16778416
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778417
    .line 16778418
    invoke-static {v0}, LX/0AGB;->LJIJJLI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 16778419
    .line 16778420
    .line 16778421
    move-result-object v0

    .line 16778422
    invoke-virtual {v1, v0}, LX/07QD;->LIZIZ(Ljava/lang/String;)Ljava/lang/String;

    .line 16778423
    .line 16778424
    .line 16778425
    move-result-object v1

    .line 16778426
    const-string v0, "log_pb"

    .line 16778427
    .line 16778428
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778429
    .line 16778430
    .line 16778431
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778432
    .line 16778433
    if-eqz v0, :cond_4c7

    .line 16778434
    .line 16778435
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16778436
    .line 16778437
    .line 16778438
    move-result-object v5

    .line 16778439
    :cond_4c7
    const-string v0, "group_id"

    .line 16778440
    .line 16778441
    invoke-virtual {v9, v0, v5}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778442
    .line 16778443
    .line 16778444
    iget v1, p0, LX/08Vk;->LLL:I

    .line 16778445
    .line 16778446
    const/16 v5, 0x3e8

    .line 16778447
    .line 16778448
    mul-int/2addr v1, v5

    .line 16778449
    const-string v0, "item_duration"

    .line 16778450
    .line 16778451
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778452
    .line 16778453
    .line 16778454
    iget v0, p0, LX/08Vk;->LLLIZZ:I

    .line 16778455
    .line 16778456
    invoke-virtual {p0, v0}, LX/08Vk;->LJIIIZ(I)I

    .line 16778457
    .line 16778458
    .line 16778459
    move-result v1

    .line 16778460
    mul-int/2addr v1, v5

    .line 16778461
    const-string/jumbo v0, "start_ts"

    .line 16778462
    .line 16778463
    .line 16778464
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778465
    .line 16778466
    .line 16778467
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16778468
    .line 16778469
    .line 16778470
    move-result v0

    .line 16778471
    invoke-virtual {p0, v0}, LX/08Vk;->LJIIIZ(I)I

    .line 16778472
    .line 16778473
    .line 16778474
    move-result v1

    .line 16778475
    mul-int/2addr v1, v5

    .line 16778476
    const-string v0, "end_ts"

    .line 16778477
    .line 16778478
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778479
    .line 16778480
    .line 16778481
    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 16778482
    .line 16778483
    .line 16778484
    move-result-object v1

    .line 16778485
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778486
    .line 16778487
    invoke-static {v1, v0}, LX/0A7u;->LJJII(Landroid/content/Context;Lcom/ss/android/ugc/aweme/feed/model/Aweme;)I

    .line 16778488
    .line 16778489
    .line 16778490
    move-result v1

    .line 16778491
    const-string v0, "is_landscape_screen"

    .line 16778492
    .line 16778493
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778494
    .line 16778495
    .line 16778496
    const-string v0, "click_method"

    .line 16778497
    .line 16778498
    invoke-virtual {v9, v0, v10}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778499
    .line 16778500
    .line 16778501
    sget-boolean v1, LX/03Bd;->LJIIJJI:Z

    .line 16778502
    .line 16778503
    const-string v0, "is_background"

    .line 16778504
    .line 16778505
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778506
    .line 16778507
    .line 16778508
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778509
    .line 16778510
    invoke-static {v0}, LX/0AGg;->LJIJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16778511
    .line 16778512
    .line 16778513
    move-result v1

    .line 16778514
    const-string v0, "has_subtitle"

    .line 16778515
    .line 16778516
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778517
    .line 16778518
    .line 16778519
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778520
    .line 16778521
    invoke-static {v0}, LX/0AGg;->LJIILLIIL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)I

    .line 16778522
    .line 16778523
    .line 16778524
    move-result v1

    .line 16778525
    const-string/jumbo v0, "use_transl"

    .line 16778526
    .line 16778527
    .line 16778528
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778529
    .line 16778530
    .line 16778531
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778532
    .line 16778533
    invoke-static {v0}, LX/0AGg;->LJIIL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 16778534
    .line 16778535
    .line 16778536
    move-result-object v1

    .line 16778537
    const-string/jumbo v0, "subtitle_type"

    .line 16778538
    .line 16778539
    .line 16778540
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778541
    .line 16778542
    .line 16778543
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778544
    .line 16778545
    invoke-static {v0}, LX/0AGg;->LJIILJJIL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 16778546
    .line 16778547
    .line 16778548
    move-result-object v1

    .line 16778549
    const-string/jumbo v0, "subtitle_source_lang"

    .line 16778550
    .line 16778551
    .line 16778552
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778553
    .line 16778554
    .line 16778555
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778556
    .line 16778557
    invoke-static {v0}, LX/0AGg;->LJIILIIL(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/String;

    .line 16778558
    .line 16778559
    .line 16778560
    move-result-object v1

    .line 16778561
    const-string/jumbo v0, "subtitle_lang"

    .line 16778562
    .line 16778563
    .line 16778564
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778565
    .line 16778566
    .line 16778567
    const-string/jumbo v0, "vDuration"

    .line 16778568
    .line 16778569
    .line 16778570
    invoke-virtual {v9, v7, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778571
    .line 16778572
    .line 16778573
    const-string/jumbo v1, "vLoops"

    .line 16778574
    .line 16778575
    .line 16778576
    sget v0, LX/08Vk;->LLLLJ:I

    .line 16778577
    .line 16778578
    invoke-virtual {v9, v0, v1}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778579
    .line 16778580
    .line 16778581
    const-string/jumbo v1, "slide_cnt"

    .line 16778582
    .line 16778583
    .line 16778584
    iget v0, p0, LX/08Vk;->LLLJL:I

    .line 16778585
    .line 16778586
    invoke-virtual {v9, v0, v1}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778587
    .line 16778588
    .line 16778589
    iget-object v1, p0, LX/08Vk;->LLLIIII:Ljava/lang/String;

    .line 16778590
    .line 16778591
    const-string/jumbo v0, "search_id"

    .line 16778592
    .line 16778593
    .line 16778594
    invoke-virtual {v9, v0, v1}, LX/05oI;->LJIIIIZZ(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778595
    .line 16778596
    .line 16778597
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16778598
    .line 16778599
    .line 16778600
    move-result v12

    .line 16778601
    if-eqz v7, :cond_591

    .line 16778602
    .line 16778603
    iget v0, p0, LX/08Vk;->LLLIZZ:I

    .line 16778604
    .line 16778605
    invoke-virtual {p0, v0}, LX/08Vk;->LJIIIZ(I)I

    .line 16778606
    .line 16778607
    .line 16778608
    move-result v0

    .line 16778609
    int-to-float v1, v0

    .line 16778610
    int-to-float v10, v5

    .line 16778611
    mul-float/2addr v1, v10

    .line 16778612
    int-to-float v7, v7

    .line 16778613
    div-float/2addr v1, v7

    .line 16778614
    const-string/jumbo v0, "vPct"

    .line 16778615
    .line 16778616
    .line 16778617
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZIZ(FLjava/lang/String;)V

    .line 16778618
    .line 16778619
    .line 16778620
    iget-wide v0, p0, LX/08Vk;->LLLL:J

    .line 16778621
    .line 16778622
    long-to-float v11, v0

    .line 16778623
    div-float/2addr v11, v7

    .line 16778624
    const-string v0, "last_pct"

    .line 16778625
    .line 16778626
    invoke-virtual {v9, v11, v0}, LX/05oI;->LIZIZ(FLjava/lang/String;)V

    .line 16778627
    .line 16778628
    .line 16778629
    invoke-virtual {p0, v12}, LX/08Vk;->LJIIIZ(I)I

    .line 16778630
    .line 16778631
    .line 16778632
    move-result v0

    .line 16778633
    int-to-float v1, v0

    .line 16778634
    mul-float/2addr v1, v10

    .line 16778635
    div-float/2addr v1, v7

    .line 16778636
    const-string v0, "now_pct"

    .line 16778637
    .line 16778638
    invoke-virtual {v9, v1, v0}, LX/05oI;->LIZIZ(FLjava/lang/String;)V

    .line 16778639
    .line 16778640
    .line 16778641
    :cond_591
    invoke-virtual {v6, v9}, LX/0450;->LJJIJ(Ljava/lang/Object;)Ljava/lang/Object;

    .line 16778642
    .line 16778643
    .line 16778644
    move-result-object v1

    .line 16778645
    invoke-static {}, LX/03AW;->LIZIZ()LX/05ue;

    .line 16778646
    .line 16778647
    .line 16778648
    move-result-object v0

    .line 16778649
    if-eqz v0, :cond_6f5

    .line 16778650
    .line 16778651
    invoke-virtual {v0}, LX/05ue;->LIZIZ()Landroid/app/Activity;

    .line 16778652
    .line 16778653
    .line 16778654
    move-result-object v0

    .line 16778655
    :goto_59f
    invoke-static {v0}, LX/089P;->LIZ(Landroid/content/Context;)LX/05g3;

    .line 16778656
    .line 16778657
    .line 16778658
    move-result-object v0

    .line 16778659
    invoke-virtual {v6, v1, v0}, LX/0450;->LJIJJ(Ljava/lang/Object;LX/05g3;)Ljava/lang/Object;

    .line 16778660
    .line 16778661
    .line 16778662
    move-result-object v6

    .line 16778663
    check-cast v6, LX/05oI;

    .line 16778664
    .line 16778665
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778666
    .line 16778667
    if-eqz v0, :cond_6f2

    .line 16778668
    .line 16778669
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getPodcastInfo()Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;

    .line 16778670
    .line 16778671
    .line 16778672
    move-result-object v0

    .line 16778673
    if-eqz v0, :cond_6f2

    .line 16778674
    .line 16778675
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;->isPodcastPreview()Ljava/lang/Boolean;

    .line 16778676
    .line 16778677
    .line 16778678
    move-result-object v1

    .line 16778679
    sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;

    .line 16778680
    .line 16778681
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16778682
    .line 16778683
    .line 16778684
    move-result v1

    .line 16778685
    :goto_5bd
    const-string v0, "is_podcast_preview"

    .line 16778686
    .line 16778687
    if-eqz v1, :cond_6ed

    .line 16778688
    .line 16778689
    invoke-virtual {v6, v3, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778690
    .line 16778691
    .line 16778692
    :goto_5c4
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778693
    .line 16778694
    if-eqz v0, :cond_6ea

    .line 16778695
    .line 16778696
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getPodcastInfo()Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;

    .line 16778697
    .line 16778698
    .line 16778699
    move-result-object v0

    .line 16778700
    if-eqz v0, :cond_6ea

    .line 16778701
    .line 16778702
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;->getPlayAddr()Lcom/ss/android/ugc/aweme/feed/model/PodcastUrlModel;

    .line 16778703
    .line 16778704
    .line 16778705
    move-result-object v0

    .line 16778706
    if-eqz v0, :cond_6ea

    .line 16778707
    .line 16778708
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastUrlModel;->getUrlList()Ljava/util/List;

    .line 16778709
    .line 16778710
    .line 16778711
    move-result-object v0

    .line 16778712
    if-eqz v0, :cond_6ea

    .line 16778713
    .line 16778714
    invoke-interface {v0}, Ljava/util/Collection;->isEmpty()Z

    .line 16778715
    .line 16778716
    .line 16778717
    move-result v0

    .line 16778718
    if-nez v0, :cond_6ea

    .line 16778719
    .line 16778720
    const/4 v0, 0x0

    .line 16778721
    :goto_5e1
    const-string v1, "podcast_rss_feed_url"

    .line 16778722
    .line 16778723
    if-nez v0, :cond_6e5

    .line 16778724
    .line 16778725
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778726
    .line 16778727
    if-eqz v0, :cond_6e2

    .line 16778728
    .line 16778729
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getPodcastInfo()Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;

    .line 16778730
    .line 16778731
    .line 16778732
    move-result-object v0

    .line 16778733
    if-eqz v0, :cond_6e2

    .line 16778734
    .line 16778735
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;->getPlayAddr()Lcom/ss/android/ugc/aweme/feed/model/PodcastUrlModel;

    .line 16778736
    .line 16778737
    .line 16778738
    move-result-object v0

    .line 16778739
    if-eqz v0, :cond_6e2

    .line 16778740
    .line 16778741
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastUrlModel;->getUrlList()Ljava/util/List;

    .line 16778742
    .line 16778743
    .line 16778744
    move-result-object v0

    .line 16778745
    if-eqz v0, :cond_6e2

    .line 16778746
    .line 16778747
    invoke-static {v0, v4}, Lcom/bytedance/mt/protector/impl/collections/ListProtector;->get(Ljava/util/List;I)Ljava/lang/Object;

    .line 16778748
    .line 16778749
    .line 16778750
    move-result-object v0

    .line 16778751
    check-cast v0, Ljava/lang/String;

    .line 16778752
    .line 16778753
    :goto_601
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    .line 16778754
    .line 16778755
    .line 16778756
    move-result v0

    .line 16778757
    if-nez v0, :cond_6e5

    .line 16778758
    .line 16778759
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778760
    .line 16778761
    if-eqz v0, :cond_6df

    .line 16778762
    .line 16778763
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getPodcastInfo()Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;

    .line 16778764
    .line 16778765
    .line 16778766
    move-result-object v0

    .line 16778767
    if-eqz v0, :cond_6df

    .line 16778768
    .line 16778769
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;->getPlayAddr()Lcom/ss/android/ugc/aweme/feed/model/PodcastUrlModel;

    .line 16778770
    .line 16778771
    .line 16778772
    move-result-object v0

    .line 16778773
    if-eqz v0, :cond_6df

    .line 16778774
    .line 16778775
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastUrlModel;->getUrlList()Ljava/util/List;

    .line 16778776
    .line 16778777
    .line 16778778
    move-result-object v0

    .line 16778779
    if-eqz v0, :cond_6df

    .line 16778780
    .line 16778781
    invoke-static {v0, v4}, Lcom/bytedance/mt/protector/impl/collections/ListProtector;->get(Ljava/util/List;I)Ljava/lang/Object;

    .line 16778782
    .line 16778783
    .line 16778784
    move-result-object v0

    .line 16778785
    check-cast v0, Ljava/lang/String;

    .line 16778786
    .line 16778787
    :goto_623
    invoke-virtual {v6, v1, v0}, LX/05oI;->LJFF(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778788
    .line 16778789
    .line 16778790
    :goto_626
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778791
    .line 16778792
    if-eqz v0, :cond_636

    .line 16778793
    .line 16778794
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getPodcastInfo()Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;

    .line 16778795
    .line 16778796
    .line 16778797
    move-result-object v0

    .line 16778798
    if-eqz v0, :cond_636

    .line 16778799
    .line 16778800
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/PodcastInfo;->getEpisodeItemId()Ljava/lang/String;

    .line 16778801
    .line 16778802
    .line 16778803
    move-result-object v1

    .line 16778804
    if-nez v1, :cond_637

    .line 16778805
    .line 16778806
    :cond_636
    move-object v1, v8

    .line 16778807
    :cond_637
    const-string v0, "podcast_rss_entry_id"

    .line 16778808
    .line 16778809
    invoke-virtual {v6, v0, v1}, LX/05oI;->LJFF(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778810
    .line 16778811
    .line 16778812
    sget-object v7, LX/08XF;->LIZ:LX/08XF;

    .line 16778813
    .line 16778814
    if-eqz v7, :cond_6dc

    .line 16778815
    .line 16778816
    invoke-virtual {v7}, LX/08XF;->isConnected()Z

    .line 16778817
    .line 16778818
    .line 16778819
    move-result v0

    .line 16778820
    :goto_644
    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    .line 16778821
    .line 16778822
    .line 16778823
    move-result-object v1

    .line 16778824
    const-string v0, "is_casting"

    .line 16778825
    .line 16778826
    invoke-virtual {v6, v1, v0}, LX/05oI;->LJ(Ljava/lang/Object;Ljava/lang/String;)V

    .line 16778827
    .line 16778828
    .line 16778829
    if-eqz v7, :cond_656

    .line 16778830
    .line 16778831
    invoke-virtual {v7}, LX/08XF;->LIZIZ()Ljava/lang/String;

    .line 16778832
    .line 16778833
    .line 16778834
    move-result-object v0

    .line 16778835
    if-eqz v0, :cond_656

    .line 16778836
    .line 16778837
    move-object v8, v0

    .line 16778838
    :cond_656
    const-string v0, "casting_session_id"

    .line 16778839
    .line 16778840
    invoke-virtual {v6, v0, v8}, LX/05oI;->LJFF(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778841
    .line 16778842
    .line 16778843
    sget-object v0, LX/0ADW;->LJ:LX/02pj;

    .line 16778844
    .line 16778845
    invoke-static {}, LX/0ADY;->LIZ()LX/0ADW;

    .line 16778846
    .line 16778847
    .line 16778848
    move-result-object v1

    .line 16778849
    iget-object v0, p0, LX/08Vk;->LLLII:Ljava/lang/String;

    .line 16778850
    .line 16778851
    invoke-virtual {v1, v0}, LX/0ADW;->LIZJ(Ljava/lang/String;)Z

    .line 16778852
    .line 16778853
    .line 16778854
    move-result v1

    .line 16778855
    const-string v0, "is_clear_screen"

    .line 16778856
    .line 16778857
    invoke-virtual {v6, v1, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778858
    .line 16778859
    .line 16778860
    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 16778861
    .line 16778862
    .line 16778863
    move-result-object v0

    .line 16778864
    invoke-static {v0}, LX/089P;->LIZ(Landroid/content/Context;)LX/05g3;

    .line 16778865
    .line 16778866
    .line 16778867
    move-result-object v1

    .line 16778868
    const/4 v0, 0x0

    .line 16778869
    invoke-static {v6, v0, v1}, LX/0CAi;->LIZ(LX/05oI;Landroid/content/Context;LX/05g3;)V

    .line 16778870
    .line 16778871
    .line 16778872
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 16778873
    .line 16778874
    .line 16778875
    move-result-object v1

    .line 16778876
    const-class v0, Lcom/ss/android/ugc/aweme/search/service/ISearchTrackingService;

    .line 16778877
    .line 16778878
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 16778879
    .line 16778880
    .line 16778881
    move-result-object v0

    .line 16778882
    check-cast v0, Lcom/ss/android/ugc/aweme/search/service/ISearchTrackingService;

    .line 16778883
    .line 16778884
    invoke-interface {v0}, Lcom/ss/android/ugc/aweme/search/service/ISearchTrackingService;->LJIJI()Ljava/util/Map;

    .line 16778885
    .line 16778886
    .line 16778887
    move-result-object v0

    .line 16778888
    invoke-virtual {v6, v0}, LX/05oI;->LJII(Ljava/util/Map;)V

    .line 16778889
    .line 16778890
    .line 16778891
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778892
    .line 16778893
    invoke-static {v6, v0}, LX/0BzI;->LIZIZ(Ljava/lang/Object;Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Ljava/lang/Object;

    .line 16778894
    .line 16778895
    .line 16778896
    invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;

    .line 16778897
    .line 16778898
    .line 16778899
    move-result-object v0

    .line 16778900
    invoke-static {v0}, LX/089P;->LIZ(Landroid/content/Context;)LX/05g3;

    .line 16778901
    .line 16778902
    .line 16778903
    move-result-object v1

    .line 16778904
    sget-object v0, Lcom/ss/android/ugc/aweme/tako/mob/TakoAIVideoMobFiller;->INSTANCE:Lcom/ss/android/ugc/aweme/tako/mob/TakoAIVideoMobFiller;

    .line 16778905
    .line 16778906
    invoke-virtual {v0, v6, v1}, Lcom/ss/android/ugc/aweme/tako/mob/TakoAIVideoMobFiller;->appendFeedParams(LX/05oI;LX/05g3;)V

    .line 16778907
    .line 16778908
    .line 16778909
    iget-object v1, v6, LX/05oI;->LIZ:Ljava/util/Map;

    .line 16778910
    .line 16778911
    const-string v0, "click_progress_bar"

    .line 16778912
    .line 16778913
    invoke-static {v0, v1}, LX/042d;->LJIIZILJ(Ljava/lang/String;Ljava/util/Map;)V

    .line 16778914
    .line 16778915
    .line 16778916
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16778917
    .line 16778918
    .line 16778919
    move-result v9

    .line 16778920
    invoke-virtual {p1}, Landroid/widget/ProgressBar;->getProgress()I

    .line 16778921
    .line 16778922
    .line 16778923
    move-result v0

    .line 16778924
    invoke-virtual {p0, v0}, LX/08Vk;->LJIIIZ(I)I

    .line 16778925
    .line 16778926
    .line 16778927
    move-result v1

    .line 16778928
    mul-int/2addr v1, v5

    .line 16778929
    iget v0, p0, LX/08Vk;->LLLIZZ:I

    .line 16778930
    .line 16778931
    invoke-virtual {p0, v0}, LX/08Vk;->LJIIIZ(I)I

    .line 16778932
    .line 16778933
    .line 16778934
    move-result v0

    .line 16778935
    mul-int/2addr v0, v5

    .line 16778936
    sub-int/2addr v1, v0

    .line 16778937
    invoke-static {v1}, Ljava/lang/Math;->abs(I)I

    .line 16778938
    .line 16778939
    .line 16778940
    move-result v10

    .line 16778941
    invoke-static {}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->get()Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;

    .line 16778942
    .line 16778943
    .line 16778944
    move-result-object v1

    .line 16778945
    const-class v0, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;

    .line 16778946
    .line 16778947
    invoke-virtual {v1, v0}, Lcom/ss/android/ugc/aweme/framework/services/ServiceManager;->getService(Ljava/lang/Class;)Ljava/lang/Object;

    .line 16778948
    .line 16778949
    .line 16778950
    move-result-object v5

    .line 16778951
    check-cast v5, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;

    .line 16778952
    .line 16778953
    iget-object v0, p0, LX/08Vk;->LLLI:Lcom/ss/android/ugc/aweme/feed/panel/IBaseListFragmentPanel;

    .line 16778954
    .line 16778955
    if-eqz v0, :cond_6da

    .line 16778956
    .line 16778957
    invoke-interface {v0, v3}, Lcom/ss/android/ugc/aweme/feed/controller/IFeedMobParamsProvider;->U7(Z)Ljava/lang/String;

    .line 16778958
    .line 16778959
    .line 16778960
    move-result-object v8

    .line 16778961
    :goto_6d1
    iget-object v6, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16778962
    .line 16778963
    iget v7, p0, LX/08Vk;->LLLIZZ:I

    .line 16778964
    .line 16778965
    invoke-interface/range {v5 .. v10}, Lcom/ss/android/ugc/aweme/search/service/ISearchVerticalDomainService;->LJIJ(Lcom/ss/android/ugc/aweme/feed/model/Aweme;ILjava/lang/String;II)V

    .line 16778966
    .line 16778967
    .line 16778968
    goto/16 :goto_1c4

    .line 16778969
    .line 16778970
    :cond_6da
    const/4 v8, 0x0

    .line 16778971
    goto :goto_6d1

    .line 16778972
    :cond_6dc
    const/4 v0, 0x0

    .line 16778973
    goto/16 :goto_644

    .line 16778974
    .line 16778975
    :cond_6df
    const/4 v0, 0x0

    .line 16778976
    goto/16 :goto_623

    .line 16778977
    .line 16778978
    :cond_6e2
    const/4 v0, 0x0

    .line 16778979
    goto/16 :goto_601

    .line 16778980
    .line 16778981
    :cond_6e5
    invoke-virtual {v6, v1, v8}, LX/05oI;->LJFF(Ljava/lang/String;Ljava/lang/String;)V

    .line 16778982
    .line 16778983
    .line 16778984
    goto/16 :goto_626

    .line 16778985
    .line 16778986
    :cond_6ea
    const/4 v0, 0x1

    .line 16778987
    goto/16 :goto_5e1

    .line 16778988
    .line 16778989
    :cond_6ed
    invoke-virtual {v6, v4, v0}, LX/05oI;->LIZJ(ILjava/lang/String;)V

    .line 16778990
    .line 16778991
    .line 16778992
    goto/16 :goto_5c4

    .line 16778993
    .line 16778994
    :cond_6f2
    const/4 v1, 0x0

    .line 16778995
    goto/16 :goto_5bd

    .line 16778996
    .line 16778997
    :cond_6f5
    const/4 v0, 0x0

    .line 16778998
    goto/16 :goto_59f

    .line 16778999
    .line 16779000
    :cond_6f8
    const/4 v7, 0x0

    .line 16779001
    goto/16 :goto_479

    .line 16779002
    .line 16779003
    :cond_6fb
    const-string/jumbo v10, "swipe_back"

    .line 16779004
    .line 16779005
    .line 16779006
    goto/16 :goto_467

    .line 16779007
    .line 16779008
    :cond_700
    const-string v12, "front"

    .line 16779009
    .line 16779010
    goto/16 :goto_462

    .line 16779011
    .line 16779012
    :cond_704
    move-object v6, v5

    .line 16779013
    :cond_705
    const/4 v0, 0x0

    .line 16779014
    goto/16 :goto_181

    .line 16779015
    .line 16779016
    :cond_708
    const/4 v9, 0x0

    .line 16779017
    goto/16 :goto_130

    .line 16779018
    .line 16779019
    :cond_70b
    const/4 v10, 0x0

    .line 16779020
    goto/16 :goto_11f

    .line 16779021
    .line 16779022
    :cond_70e
    const/4 v2, 0x0

    .line 16779023
    goto/16 :goto_118

    .line 16779024
    .line 16779025
    :cond_711
    const/4 v0, 0x0

    .line 16779026
    goto/16 :goto_112

    .line 16779027
    .line 16779028
    :cond_714
    new-instance v1, LX/1Plu;

    .line 16779029
    .line 16779030
    invoke-direct {v1, v6, v5, v4, v2}, LX/1Plu;-><init>(Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;Lcom/ss/android/ugc/aweme/feed/longvideo/edgespeedup/EdgeSpeedupAbility;Ljava/lang/String;Ljava/lang/String;)V

    .line 16779031
    .line 16779032
    .line 16779033
    new-instance v0, LX/1Plx;

    .line 16779034
    .line 16779035
    invoke-direct {v0, v6}, LX/1Plx;-><init>(Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;)V

    .line 16779036
    .line 16779037
    .line 16779038
    iput-object v0, v1, LX/1Plu;->LJIILIIL:Lkotlin/jvm/functions/Function0;

    .line 16779039
    .line 16779040
    invoke-virtual {v1}, LX/1Plu;->LJI()V

    .line 16779041
    .line 16779042
    .line 16779043
    iput-object v1, v6, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->LLLFF:LX/1Plu;

    .line 16779044
    .line 16779045
    return-void
.end method

.method public final onVideoEvent(LX/08Wq;)V
    .registers 6
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    iget v1, p1, LX/08Wq;->LIZ:I

    .line 16777217
    .line 16777218
    const/16 v0, 0x4c

    .line 16777219
    .line 16777220
    if-eq v1, v0, :cond_3a

    .line 16777221
    .line 16777222
    const/16 v0, 0x50

    .line 16777223
    .line 16777224
    const/4 v3, 0x0

    .line 16777225
    if-eq v1, v0, :cond_6e

    .line 16777226
    .line 16777227
    const/16 v0, 0x53

    .line 16777228
    .line 16777229
    if-ne v1, v0, :cond_39

    .line 16777230
    .line 16777231
    invoke-static {}, LX/089i;->LIZIZ()Z

    .line 16777232
    .line 16777233
    .line 16777234
    move-result v0

    .line 16777235
    if-eqz v0, :cond_39

    .line 16777236
    .line 16777237
    iget-object v1, p1, LX/08Wq;->LIZIZ:Ljava/lang/Object;

    .line 16777238
    .line 16777239
    instance-of v0, v1, LX/08uZ;

    .line 16777240
    .line 16777241
    if-eqz v0, :cond_6d

    .line 16777242
    .line 16777243
    check-cast v1, LX/08uZ;

    .line 16777244
    .line 16777245
    if-eqz v1, :cond_6d

    .line 16777246
    .line 16777247
    iget-boolean v1, v1, LX/08uZ;->LIZ:Z

    .line 16777248
    .line 16777249
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777250
    .line 16777251
    invoke-virtual {v0}, LX/08Ic;->getSeekBarShowType()I

    .line 16777252
    .line 16777253
    .line 16777254
    move-result v0

    .line 16777255
    const/4 v2, 0x1

    .line 16777256
    if-eq v0, v2, :cond_39

    .line 16777257
    .line 16777258
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777259
    .line 16777260
    invoke-virtual {p0, v0}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777261
    .line 16777262
    .line 16777263
    move-result v0

    .line 16777264
    if-eqz v0, :cond_63

    .line 16777265
    .line 16777266
    if-eqz v1, :cond_63

    .line 16777267
    .line 16777268
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777269
    .line 16777270
    invoke-virtual {v0, v3}, LX/08Ic;->setSeekBarShowType(I)V

    .line 16777271
    .line 16777272
    .line 16777273
    :cond_39
    return-void

    .line 16777274
    :cond_3a
    invoke-static {}, LX/0AAT;->LIZ()Z

    .line 16777275
    .line 16777276
    .line 16777277
    move-result v0

    .line 16777278
    if-eqz v0, :cond_39

    .line 16777279
    .line 16777280
    iget-object v1, p1, LX/08Wq;->LIZIZ:Ljava/lang/Object;

    .line 16777281
    .line 16777282
    instance-of v0, v1, LX/08uZ;

    .line 16777283
    .line 16777284
    if-eqz v0, :cond_39

    .line 16777285
    .line 16777286
    check-cast v1, LX/08uZ;

    .line 16777287
    .line 16777288
    if-eqz v1, :cond_39

    .line 16777289
    .line 16777290
    iget-boolean v2, v1, LX/08uZ;->LIZ:Z

    .line 16777291
    .line 16777292
    iget-object v0, p0, LX/08Vk;->LLJJJJ:Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;

    .line 16777293
    .line 16777294
    if-eqz v0, :cond_53

    .line 16777295
    .line 16777296
    invoke-virtual {v0, v2}, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->setDisableSeekTouch(Z)V

    .line 16777297
    .line 16777298
    .line 16777299
    :cond_53
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777300
    .line 16777301
    invoke-virtual {v0, v2}, LX/08Ic;->setDisableSeekTouch(Z)V

    .line 16777302
    .line 16777303
    .line 16777304
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777305
    .line 16777306
    new-instance v0, LX/0iBE;

    .line 16777307
    .line 16777308
    invoke-direct {v0, v2}, LX/0iBE;-><init>(Z)V

    .line 16777309
    .line 16777310
    .line 16777311
    invoke-virtual {v1, v0}, LX/05f6;->LJIIL(Lkotlin/jvm/functions/Function0;)V

    .line 16777312
    .line 16777313
    .line 16777314
    return-void

    .line 16777315
    :cond_63
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777316
    .line 16777317
    invoke-virtual {p0, v2}, LX/08Vk;->LJIIJJI(Z)I

    .line 16777318
    .line 16777319
    .line 16777320
    move-result v0

    .line 16777321
    invoke-virtual {v1, v0}, LX/08Ic;->setSeekBarShowType(I)V

    .line 16777322
    .line 16777323
    .line 16777324
    return-void

    .line 16777325
    :cond_6d
    return-void

    .line 16777326
    :cond_6e
    iget-object v1, p1, LX/08Wq;->LIZIZ:Ljava/lang/Object;

    .line 16777327
    .line 16777328
    instance-of v0, v1, Ljava/lang/Boolean;

    .line 16777329
    .line 16777330
    if-eqz v0, :cond_7c

    .line 16777331
    .line 16777332
    check-cast v1, Ljava/lang/Boolean;

    .line 16777333
    .line 16777334
    if-eqz v1, :cond_7c

    .line 16777335
    .line 16777336
    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z

    .line 16777337
    .line 16777338
    .line 16777339
    move-result v3

    .line 16777340
    :cond_7c
    iget-object v0, p0, LX/08Vk;->LLJJJJ:Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;

    .line 16777341
    .line 16777342
    if-eqz v0, :cond_83

    .line 16777343
    .line 16777344
    invoke-virtual {v0, v3}, Lcom/ss/android/ugc/aweme/feed/ui/seekbar/VideoSeekBarMaskView;->setDisableSeekTouch(Z)V

    .line 16777345
    .line 16777346
    .line 16777347
    :cond_83
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777348
    .line 16777349
    invoke-virtual {v0, v3}, LX/08Ic;->setDisableSeekTouch(Z)V

    .line 16777350
    .line 16777351
    .line 16777352
    if-eqz v3, :cond_90

    .line 16777353
    .line 16777354
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777355
    .line 16777356
    invoke-static {v0}, LX/08Vb;->LIZIZ(Landroid/view/View;)V

    .line 16777357
    .line 16777358
    .line 16777359
    return-void

    .line 16777360
    :cond_90
    iget-object v0, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777361
    .line 16777362
    invoke-static {v0}, LX/08Vb;->LIZLLL(Landroid/view/View;)V

    .line 16777363
    .line 16777364
    .line 16777365
    return-void
.end method

.method public final onVideoMaskEvent(LX/08Wr;)V
    .registers 4
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    iget-object v1, p1, LX/08Wr;->LIZIZ:Ljava/lang/String;

    .line 16777217
    .line 16777218
    iget-object v0, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777219
    .line 16777220
    if-eqz v0, :cond_34

    .line 16777221
    .line 16777222
    invoke-virtual {v0}, Lcom/ss/android/ugc/aweme/feed/model/Aweme;->getAid()Ljava/lang/String;

    .line 16777223
    .line 16777224
    .line 16777225
    move-result-object v0

    .line 16777226
    :goto_a
    invoke-static {v1, v0}, Lkotlin/jvm/internal/Intrinsics;->LJI(Ljava/lang/Object;Ljava/lang/Object;)Z

    .line 16777227
    .line 16777228
    .line 16777229
    move-result v0

    .line 16777230
    if-eqz v0, :cond_21

    .line 16777231
    .line 16777232
    iget-object v1, p0, LX/08Vk;->LLJJJJLIIL:LX/089c;

    .line 16777233
    .line 16777234
    new-instance v0, LX/0uiS;

    .line 16777235
    .line 16777236
    invoke-direct {v0, p1}, LX/0uiS;-><init>(LX/08Wr;)V

    .line 16777237
    .line 16777238
    .line 16777239
    invoke-virtual {v1, v0}, LX/05f6;->LJIIL(Lkotlin/jvm/functions/Function0;)V

    .line 16777240
    .line 16777241
    .line 16777242
    iget-boolean v0, p1, LX/08Wr;->LIZ:Z

    .line 16777243
    .line 16777244
    if-nez v0, :cond_30

    .line 16777245
    .line 16777246
    invoke-virtual {p0}, LX/08Vk;->LIZJ()V

    .line 16777247
    .line 16777248
    .line 16777249
    :cond_21
    :goto_21
    iget-boolean v0, p1, LX/08Wr;->LIZJ:Z

    .line 16777250
    .line 16777251
    if-eqz v0, :cond_2f

    .line 16777252
    .line 16777253
    invoke-virtual {p0}, LX/08Vk;->LJ()V

    .line 16777254
    .line 16777255
    .line 16777256
    invoke-virtual {p0}, LX/08Vk;->LJIIJ()LX/08WY;

    .line 16777257
    .line 16777258
    .line 16777259
    move-result-object v0

    .line 16777260
    invoke-virtual {v0}, LX/08WY;->LIZIZ()V

    .line 16777261
    .line 16777262
    .line 16777263
    :cond_2f
    return-void

    .line 16777264
    :cond_30
    invoke-virtual {p0}, LX/08Vk;->LJ()V

    .line 16777265
    .line 16777266
    .line 16777267
    goto :goto_21

    .line 16777268
    :cond_34
    const/4 v0, 0x0

    .line 16777269
    goto :goto_a
.end method

.method public final onVideoProgressVolumeKeyEvent(LX/08Ws;)V
    .registers 4
    .annotation runtime LX/04GW;
    .end annotation

    .prologue
    .line 16777216
    invoke-virtual {p0, p1}, LX/08Vk;->LJIILLIIL(Ljava/lang/Object;)Z

    .line 16777217
    .line 16777218
    .line 16777219
    move-result v0

    .line 16777220
    if-eqz v0, :cond_1c

    .line 16777221
    .line 16777222
    iget-object v0, p1, LX/08Ws;->LIZ:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 16777223
    .line 16777224
    invoke-virtual {p0, v0}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 16777225
    .line 16777226
    .line 16777227
    move-result v0

    .line 16777228
    if-eqz v0, :cond_1c

    .line 16777229
    .line 16777230
    iget-boolean v0, p0, LX/08Vk;->LLLIILIL:Z

    .line 16777231
    .line 16777232
    if-eqz v0, :cond_1c

    .line 16777233
    .line 16777234
    iget-boolean v0, p1, LX/08Ws;->LIZJ:Z

    .line 16777235
    .line 16777236
    if-eqz v0, :cond_1d

    .line 16777237
    .line 16777238
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777239
    .line 16777240
    const/4 v0, 0x4

    .line 16777241
    invoke-static {v1, v0}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777242
    .line 16777243
    .line 16777244
    :cond_1c
    return-void

    .line 16777245
    :cond_1d
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 16777246
    .line 16777247
    const/4 v0, 0x0

    .line 16777248
    invoke-static {v1, v0}, LX/00kD;->e0(LX/08Ic;I)V

    .line 16777249
    .line 16777250
    .line 16777251
    return-void
.end method

.method public final tj(ZLcom/ss/android/ugc/aweme/feed/model/Aweme;Landroid/animation/ValueAnimator;)V
    .registers 6

    .prologue
    .line 50331648
    iput-object p2, p0, LX/08Vk;->LLJLLIL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;

    .line 50331649
    .line 50331650
    if-eqz p1, :cond_b

    .line 50331651
    .line 50331652
    invoke-virtual {p0, p2}, LX/08Vk;->LJI(Lcom/ss/android/ugc/aweme/feed/model/Aweme;)Z

    .line 50331653
    .line 50331654
    .line 50331655
    move-result v0

    .line 50331656
    if-nez v0, :cond_b

    .line 50331657
    .line 50331658
    return-void

    .line 50331659
    :cond_b
    if-eqz p3, :cond_21

    .line 50331660
    .line 50331661
    new-instance v0, LX/0iBA;

    .line 50331662
    .line 50331663
    invoke-direct {v0, p0}, LX/0iBA;-><init>(LX/08Vk;)V

    .line 50331664
    .line 50331665
    .line 50331666
    invoke-virtual {p3, v0}, Landroid/animation/ValueAnimator;->addUpdateListener(Landroid/animation/ValueAnimator$AnimatorUpdateListener;)V

    .line 50331667
    .line 50331668
    .line 50331669
    new-instance v0, LX/0iBC;

    .line 50331670
    .line 50331671
    invoke-direct {v0, p1, p0}, LX/0iBC;-><init>(ZLX/08Vk;)V

    .line 50331672
    .line 50331673
    .line 50331674
    invoke-virtual {p3, v0}, Landroid/animation/Animator;->addListener(Landroid/animation/Animator$AnimatorListener;)V

    .line 50331675
    .line 50331676
    .line 50331677
    invoke-virtual {p3}, Landroid/animation/ValueAnimator;->start()V

    .line 50331678
    .line 50331679
    .line 50331680
    return-void

    .line 50331681
    :cond_21
    iget-object v1, p0, LX/08Vk;->LL:LX/08Ic;

    .line 50331682
    .line 50331683
    if-eqz p1, :cond_2a

    .line 50331684
    .line 50331685
    const/4 v0, 0x0

    .line 50331686
    :goto_26
    invoke-static {v1, v0}, LX/00kD;->e0(LX/08Ic;I)V

    .line 50331687
    .line 50331688
    .line 50331689
    return-void

    .line 50331690
    :cond_2a
    const/16 v0, 0x8

    .line 50331691
    .line 50331692
    goto :goto_26
.end method
