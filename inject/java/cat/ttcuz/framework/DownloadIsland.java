package cat.ttcuz.framework;
import android.app.*;import android.view.*;import android.widget.*;
/**
 * Expand the island into an MD3 task list, retaining its progress ring.
 *
 * The island lives in an IslandLayer: a view as big as the open island, never resized, which draws only the shape
 * ({@link DownloadMark#visual}). Opening and closing move that shape alone, frame by frame on the render thread, with
 * no window or layout pass on the way -- so nothing can run ahead of or lag behind anything else. The glass under the
 * shape pauses while it moves (the island turns solid) and comes back with a short fade once it rests.
 */
final class DownloadIsland {
    private final Activity owner;private final IslandLayer layer;private final DownloadMark surface;
    private LinearLayout rows;private ScrollView list;private android.animation.ValueAnimator motion,clearing;
    private boolean expanded,disposed;private String keys="";
    private final java.util.Map<String,Row> rendered=new java.util.LinkedHashMap<>();
    private Runnable refresh;
    /** The shape's size now (px), always set: the view itself is the open island's size. */
    private float shownW,shownH;
    private int generation;
    private static final int ON_SURFACE=0xFFF2F0F5,ON_SURFACE_VARIANT=0xB3F2F0F5,CONTAINER=0x1FFFFFFF;
    DownloadIsland(Activity owner,IslandLayer layer,DownloadMark surface){
        this.owner=owner;this.layer=layer;this.surface=surface;
        refresh=()->{if(disposed||!expanded||!this.layer.isShowing())return;update();this.surface.postDelayed(this.refresh,250L);};
        surface.setOnClickListener(v->toggle());
        shape(compactWidth(),compactHeight(),0f);
    }
    boolean expanded(){return expanded;}
    float shownWidth(){return shownW;}
    float shownHeight(){return shownH;}
    private int dp(int n){return Math.round(n*owner.getResources().getDisplayMetrics().density);}
    static int openWidth(Activity a){float d=a.getResources().getDisplayMetrics().density;return Math.min(Math.round(320*d),a.getResources().getDisplayMetrics().widthPixels-Math.round(32*d));}
    static int openHeight(Activity a){float d=a.getResources().getDisplayMetrics().density;return Math.min(Math.round(308*d),a.getResources().getDisplayMetrics().heightPixels/2);}
    private int openWidth(){return openWidth(owner);}
    private int openHeight(){return openHeight(owner);}
    private int compactWidth(){return Math.min(dp(DownloadMark.SIDE),owner.getResources().getDisplayMetrics().widthPixels-dp(32));}
    private int compactHeight(){return dp(DownloadMark.SIDE);}
    void toggle(){if(disposed)return;if(expanded){collapse(null);return;}
        expanded=true;
        if(list==null){
            keys=null;rows=new LinearLayout(owner);rows.setClickable(true);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(dp(12),0,dp(12),dp(12));
            list=new ScrollView(owner);list.setFillViewport(false);list.setVerticalScrollBarEnabled(false);list.addView(rows);list.setAlpha(0f);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,openHeight()-dp(DownloadMark.SIDE)-dp(12));p.topMargin=dp(4);surface.addView(list,p);
            update();
        }
        animate(true,null);surface.removeCallbacks(refresh);surface.postDelayed(refresh,250L);
    }
    void collapse(Runnable after){if(!expanded){if(after!=null)after.run();return;}expanded=false;surface.removeCallbacks(refresh);animate(false,after);}
    /** From wherever the shape is now (even mid-motion) to the other size: no restart from zero, no jump. */
    private void animate(final boolean opening,final Runnable after){
        if(motion!=null){motion.cancel();motion=null;}
        if(clearing!=null){clearing.cancel();clearing=null;}
        final int run=++generation;
        final float fromW=shownW,fromH=shownH;
        final int targetW=opening?openWidth():compactWidth(),targetH=opening?openHeight():compactHeight();
        final float startAlpha=list==null?0:list.getAlpha();
        if(!Motion.enabled(owner)){shape(targetW,targetH,opening?1:0);end(opening,after);return;}
        surface.moving(true);surface.solid(1f);
        motion=android.animation.ValueAnimator.ofFloat(0,1);
        motion.setDuration(Motion.time(opening?420L:340L));
        // emphasized decelerate opening, standard closing: the island lands softly and leaves without a snap
        motion.setInterpolator(opening?new android.view.animation.PathInterpolator(.05f,.7f,.1f,1f):Motion.BOTH);
        motion.addUpdateListener(a->{if(disposed)return;float t=(Float)a.getAnimatedValue();
            // the list appears once the island is most of the way open and leaves before it closes
            float fade=opening?Math.max(0f,Math.min(1f,(t-.35f)/.65f)):Math.min(1f,t/.4f);
            shape(DownloadMorph.mix(fromW,targetW,t),DownloadMorph.mix(fromH,targetH,t),DownloadMorph.mix(startAlpha,opening?1:0,fade));});
        motion.addListener(new android.animation.AnimatorListenerAdapter(){boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator a){cancelled=true;}
            @Override public void onAnimationEnd(android.animation.Animator a){if(!cancelled&&!disposed&&run==generation)end(opening,after);}});
        motion.start();
    }
    private void shape(float width,float height,float alpha){shownW=width;shownH=height;surface.visual(width,height);if(list!=null){list.setAlpha(alpha);list.setTranslationY(dp(8)*(1-alpha));}}
    private void end(final boolean opening,final Runnable after){motion=null;
        if(!opening){if(list!=null)surface.removeView(list);list=null;rows=null;rendered.clear();}
        // the glass comes back under a solid island that then clears
        surface.moving(false);
        if(Motion.enabled(owner)){
            clearing=android.animation.ValueAnimator.ofFloat(1,0);clearing.setDuration(Motion.time(220L));clearing.setInterpolator(Motion.BOTH);
            clearing.addUpdateListener(a->{if(!disposed)surface.solid((Float)a.getAnimatedValue());});
            clearing.start();
        }else surface.solid(0f);
        if(after!=null)after.run();else if(!opening)Screen.tasksCollapsed();
    }
    void dispose(){disposed=true;surface.removeCallbacks(refresh);if(motion!=null){motion.cancel();motion=null;}if(clearing!=null){clearing.cancel();clearing=null;}surface.moving(false);}

    // ------------------------------------------------------------ MD3 list

    private TextView text(String s,int size,int colour){TextView v=new TextView(owner);v.setText(s);v.setTextSize(size);v.setTextColor(colour);v.setIncludeFontPadding(false);Fonts.apply(v);return v;}
    /** A 40 dp icon button: a Google symbol on a round 48 dp touch target. */
    private ImageView button(String symbol,int colour,String label){
        ImageView v=new ImageView(owner);v.setImageDrawable(Symbols.drawable(symbol,colour));v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int pad=dp(12);v.setPadding(pad,pad,pad,pad);v.setContentDescription(label);v.setClickable(true);
        android.graphics.drawable.GradientDrawable round=new android.graphics.drawable.GradientDrawable();round.setShape(android.graphics.drawable.GradientDrawable.OVAL);round.setColor(0x00FFFFFF);
        v.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF),null,round));
        return v;
    }
    private static String symbolOf(DownloadTasks.Task t){
        if(t.state.equals(Text.t("Готово"))||t.state.equals(Text.t("Завершено")))return "check_circle_fill1";
        if(t.state.equals(Text.t("Ошибка")))return "error_fill1";
        if(t.state.equals(Text.t("Отменено")))return "close";
        return "download";
    }
    private static final class Row {TextView state;ProgressBar progress;ImageView icon,action;String symbol,actionSymbol;Symbols.Glyph glyph;}
    private void update(){if(rows==null)return;java.util.List<DownloadTasks.Task> tasks=DownloadTasks.snapshot();StringBuilder fingerprint=new StringBuilder();for(DownloadTasks.Task t:tasks)fingerprint.append(t.key).append((char)10);
        String next=fingerprint.toString();int accent=Accent.colour();
        if(!next.equals(keys)){keys=next;rows.removeAllViews();rendered.clear();
            LinearLayout header=new LinearLayout(owner);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(4),0,0,0);
            TextView title=text(Text.t("Задачи"),16,ON_SURFACE);header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));title.setGravity(Gravity.CENTER_VERTICAL);
            ImageView close=button("expand_less",ON_SURFACE,Text.t("Свернуть"));close.setOnClickListener(v->collapse(null));header.addView(close,new LinearLayout.LayoutParams(dp(48),dp(48)));
            rows.addView(header);
            if(tasks.isEmpty()){TextView empty=text(Text.t("Нет сетевых задач"),14,ON_SURFACE_VARIANT);empty.setGravity(Gravity.CENTER);empty.setMinHeight(dp(72));rows.addView(empty);}
            for(DownloadTasks.Task t:tasks){Row row=new Row();
                LinearLayout card=new LinearLayout(owner);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(4),dp(12));
                android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(CONTAINER);shape.setCornerRadius(dp(16));card.setBackground(shape);
                LinearLayout top=new LinearLayout(owner);top.setGravity(Gravity.CENTER_VERTICAL);
                row.icon=new ImageView(owner);android.graphics.drawable.GradientDrawable halo=new android.graphics.drawable.GradientDrawable();halo.setShape(android.graphics.drawable.GradientDrawable.OVAL);halo.setColor((accent&0xFFFFFF)|0x33000000);row.icon.setBackground(halo);
                row.icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);row.icon.setPadding(dp(8),dp(8),dp(8),dp(8));top.addView(row.icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
                LinearLayout words=new LinearLayout(owner);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(12),0,dp(4),0);
                TextView label=text(t.label,14,ON_SURFACE);label.setMaxLines(1);label.setEllipsize(android.text.TextUtils.TruncateAt.END);words.addView(label);
                row.state=text("",12,ON_SURFACE_VARIANT);row.state.setMaxLines(1);row.state.setEllipsize(android.text.TextUtils.TruncateAt.END);row.state.setPadding(0,dp(4),0,0);words.addView(row.state);
                top.addView(words,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
                row.action=button("close",ON_SURFACE,Text.t("Отменить"));top.addView(row.action,new LinearLayout.LayoutParams(dp(48),dp(48)));
                card.addView(top);
                row.progress=new ProgressBar(owner,null,android.R.attr.progressBarStyleHorizontal);row.progress.setMax(100);row.progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));row.progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf((accent&0xFFFFFF)|0x40000000));
                LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(4));bar.topMargin=dp(10);bar.rightMargin=dp(8);card.addView(row.progress,bar);
                LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);gap.bottomMargin=dp(8);rows.addView(card,gap);rendered.put(t.key,row);
            }
        }
        for(DownloadTasks.Task t:tasks){Row row=rendered.get(t.key);if(row==null)continue;
            boolean loading=t.state.equals(Text.t("Загрузка"));
            String state=t.origin+" · "+t.owner+" · "+t.state+(t.percent>=0&&loading?" · "+t.percent+"%":"");if(!state.contentEquals(row.state.getText()))row.state.setText(state);
            String symbol=symbolOf(t);if(!symbol.equals(row.symbol)){row.symbol=symbol;row.icon.setImageDrawable(Symbols.drawable(symbol,accent));}
            boolean indeterminate=t.percent<0&&loading;if(row.progress.isIndeterminate()!=indeterminate)row.progress.setIndeterminate(indeterminate);if(t.percent>=0)row.progress.setProgress(t.percent);else if(!loading)row.progress.setProgress(100);
            row.action.setVisibility(t.cancel||t.retry?View.VISIBLE:View.INVISIBLE);
            String act=t.cancel?"close":"refresh";if(!act.equals(row.actionSymbol)){row.actionSymbol=act;row.action.setImageDrawable(Symbols.drawable(act,ON_SURFACE));row.action.setContentDescription(t.cancel?Text.t("Отменить"):Text.t("Повторить"));}
            final String key=t.key;final boolean cancel=t.cancel;row.action.setOnClickListener(v->{if(cancel)DownloadTasks.cancel(key);else DownloadTasks.retry(key);update();});
        }
    }
}
