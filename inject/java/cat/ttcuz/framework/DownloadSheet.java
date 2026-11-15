package cat.ttcuz.framework;
import android.content.Context;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.text.SpannableStringBuilder;import android.text.Spanned;import android.text.style.ForegroundColorSpan;import android.util.TypedValue;import android.view.Gravity;import android.view.View;import android.view.ViewGroup;import android.widget.FrameLayout;import android.widget.ImageView;import android.widget.LinearLayout;import android.widget.TextView;import java.util.List;
/**
 * Download chooser in Material 3 cards, with no drag handle drawn on top.
 *
 * A video post: a title, then one card per choice. The tallest size stands first with the accent-coloured
 * name, its file size and a "Best" pill. The remaining sizes (h264 and HEVC) are folded into one card that
 * says how many variants there are and opens the full list. The soundtrack is always the last card.
 * A photo post: one "all photos" card, then the photos themselves to tick and save.
 * File sizes are not in TikTok's model, so each one is asked for with a HEAD request and filled in when it
 * arrives; until then the bitrate stands in.
 */
final class DownloadSheet {
 interface Pick {void on(DownloadQuality.Choice choice);}
 private DownloadSheet(){}
 private static final java.util.Map<String,Long> sizes=new java.util.HashMap<>();

 /** One card: its view and the second line, which can change once the size is known. */
 private static final class Row {LinearLayout view;TextView sub;}

 static void show(Context c,List<DownloadQuality.Choice> videos,List<DownloadQuality.Choice> photos,boolean nativeOnly,final String audioUrl,final Panel.Click nativeSave,final Pick video,final Pick photo,final Panel.Click audio,final Panel.Click allPhotos){
  final Skin skin=Skin.remembered(c);
  final boolean photoPost=photos!=null&&!photos.isEmpty();
  // no heading of the panel's own: the title is centred inside the content, and nothing is drawn as a handle
  // edge to edge along the bottom, rather than a floating window
  final Panel panel=Panel.with(c,skin,null).compact().side();
  final LinearLayout root=new LinearLayout(c);root.setOrientation(LinearLayout.VERTICAL);
  DownloadQuality.Choice best=null;java.util.ArrayList<DownloadQuality.Choice> rest=new java.util.ArrayList<>();
  if(videos!=null)for(DownloadQuality.Choice v:videos){if(v.best&&best==null)best=v;else rest.add(v);}
  // the "best" entry is the tallest h264 size under another name: do not list that size twice (its HEVC twin stays)
  final java.util.ArrayList<DownloadQuality.Choice> list=new java.util.ArrayList<>();
  if(best!=null)list.add(best);
  for(DownloadQuality.Choice v:rest){boolean same=best!=null&&!v.hevc&&best.height>0&&v.height==best.height;if(!same)list.add(v);}
  final java.util.ArrayList<DownloadQuality.Choice> all=list;
  final Runnable[] main=new Runnable[1];
  main[0]=()->{
   root.removeAllViews();
   root.addView(title(c,skin,photoPost?Text.SHEET_POST_TITLE:Text.DL_TITLE));
   if(photoPost){
    if(photos.size()==1){final DownloadQuality.Choice only=photos.get(0);
     root.addView(card(c,skin,panel,"image",true,Text.SAVE_IMAGE,null,"download",()->photo.on(only)).view,gap(c,6));}
    else{
     root.addView(card(c,skin,panel,"image",true,Text.allPhotos(photos.size()),null,"download",()->allPhotos.on()).view,gap(c,6));
     root.addView(photoList(c,skin,panel,photos,photo),gap(c,8));
    }
   }else if(!all.isEmpty()){
    final DownloadQuality.Choice hero=all.get(0);
    Row top=card(c,skin,panel,"play_arrow",true,heading(skin,hero,true),"",null,()->video.on(hero));
    pill(c,top.view,Text.DL_BEST);
    sizeInto(top,hero,true);
    root.addView(top.view,gap(c,6));
    final java.util.List<DownloadQuality.Choice> more=all.subList(1,all.size());
    if(more.size()==1){
     final DownloadQuality.Choice only=more.get(0);
     Row one=card(c,skin,panel,"play_arrow",false,heading(skin,only,false),"","download",()->video.on(only));
     sizeInto(one,only,true);root.addView(one.view,gap(c,8));
    }else if(more.size()>1){
     final java.util.List<DownloadQuality.Choice> options=new java.util.ArrayList<>(more);
     Row group=card(c,skin,null,"play_arrow",false,heading(skin,options.get(0),false),Text.variants(options.size()),"chevron_right",null);
     group.view.setOnClickListener(v->Motion.navigate(root,1f,()->variants(c,skin,panel,root,options,video,main[0])));
     root.addView(group.view,gap(c,8));
    }
   }else if(nativeOnly){
    root.addView(card(c,skin,panel,"hd",true,Text.DL_NATIVE,null,"download",()->nativeSave.on()).view,gap(c,6));
   }
   Row sound=card(c,skin,panel,"music_note",false,Text.DL_AUDIO,"","download",()->audio.on());
   sizeInto(sound,audioUrl,0L);
   root.addView(sound.view,gap(c,8));
   root.addView(cancel(c,skin,panel),gap(c,6));
  };
  main[0].run();
  panel.view(root);panel.show();
 }

 /** The full list of the folded sizes, as one segmented group under a header with a way back. */
 private static void variants(Context c,Skin skin,Panel panel,LinearLayout root,List<DownloadQuality.Choice> options,Pick video,Runnable back){
  root.removeAllViews();
  int accent=Accent.colour();
  FrameLayout head=new FrameLayout(c);
  FrameLayout arrow=new FrameLayout(c);arrow.setClickable(true);arrow.setFocusable(true);arrow.setContentDescription(Text.DL_BACK);
  arrow.setBackground(Expressive.tile(c,(skin.text&0xFFFFFF)|0x14000000,(accent&0xFFFFFF)|0x33000000,dp(c,20),dp(c,20),dp(c,12)));
  ImageView glyph=new ImageView(c);glyph.setImageDrawable(SettingsGlyph.make("arrow_forward",skin.text));glyph.setRotation(180f);
  arrow.addView(glyph,new FrameLayout.LayoutParams(dp(c,22),dp(c,22),Gravity.CENTER));
  Motion.touch(arrow);arrow.setOnClickListener(v->Motion.navigate(root,-1f,back));
  head.addView(arrow,new FrameLayout.LayoutParams(dp(c,40),dp(c,40),Gravity.START|Gravity.CENTER_VERTICAL));
  TextView name=title(c,skin,Text.DL_OTHER);head.addView(name,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.CENTER));
  root.addView(head,gap(c,0));
  Expressive.Group group=new Expressive.Group(c,(skin.text&0xFFFFFF)|0x14000000,(accent&0xFFFFFF)|0x33000000);
  for(final DownloadQuality.Choice item:options){
   Row row=card(c,skin,panel,"play_arrow",false,heading(skin,item,false),"","download",()->video.on(item));
   row.view.setBackground(null);sizeInto(row,item,true);
   group.addView(row.view,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
  }
  root.addView(group,gap(c,8));
 }

 // ------------------------------------------------------------------ pieces

 private static TextView title(Context c,Skin skin,String words){
  TextView head=new TextView(c);head.setText(words);head.setTextColor(skin.text);head.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
  head.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));head.setGravity(Gravity.CENTER);head.setPadding(dp(c,4),dp(c,6),dp(c,4),dp(c,12));
  return head;
 }
 private static LinearLayout.LayoutParams gap(Context c,int top){
  LinearLayout.LayoutParams where=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);where.topMargin=dp(c,top);return where;
 }
 /** "720p" and the codec after it: the size is in the accent on the leading card, the codec is quieter. */
 private static CharSequence heading(Skin skin,DownloadQuality.Choice item,boolean lead){
  String size=item.height>0?item.height+"p":item.best?Text.t("Лучшее"):Math.round(item.bitrate/1000.0)+Text.t(" кбит/с");
  String codec=item.best?"H264":item.hevc?"HEVC":"H264";
  SpannableStringBuilder out=new SpannableStringBuilder(size);
  if(lead)out.setSpan(new ForegroundColorSpan(Accent.colour()),0,size.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  int from=out.length();out.append("  ").append(codec);
  out.setSpan(new ForegroundColorSpan(skin.muted()),from,out.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  return out;
 }
 private static int onAccent(int colour){
  int red=(colour>>16)&0xFF,green=(colour>>8)&0xFF,blue=colour&0xFF;
  return (red*299+green*587+blue*114)/1000>150?0xFF111111:0xFFFFFFFF;
 }

 /** One Material 3 card: a rounded icon tile, a title, a second line and what stands at the end. */
 private static Row card(Context c,Skin skin,final Panel panel,String icon,boolean lead,CharSequence title,String sub,String trailing,final Panel.Click then){
  final int accent=Accent.colour();
  Row row=new Row();
  LinearLayout view=new LinearLayout(c);view.setOrientation(LinearLayout.HORIZONTAL);view.setGravity(Gravity.CENTER_VERTICAL);
  view.setPadding(dp(c,14),dp(c,12),dp(c,16),dp(c,12));view.setMinimumHeight(dp(c,72));
  view.setClickable(true);view.setFocusable(true);
  view.setContentDescription(sub==null||sub.length()==0?title:title+", "+sub);
  int fill=lead?(accent&0xFFFFFF)|0x2E000000:(skin.text&0xFFFFFF)|0x14000000;
  view.setBackground(Expressive.tile(c,fill,(accent&0xFFFFFF)|0x33000000,dp(c,24),dp(c,24),dp(c,16)));
  // the icon: a rounded square, filled with the accent on the leading card and tonal on the others
  FrameLayout tile=new FrameLayout(c);
  GradientDrawable square=new GradientDrawable();square.setCornerRadius(dp(c,14));
  square.setColor(lead?accent:(accent&0xFFFFFF)|0x2E000000);tile.setBackground(square);
  ImageView glyph=new ImageView(c);glyph.setImageDrawable(SettingsGlyph.make(icon,lead?onAccent(accent):accent));
  tile.addView(glyph,new FrameLayout.LayoutParams(dp(c,24),dp(c,24),Gravity.CENTER));
  LinearLayout.LayoutParams tileWhere=new LinearLayout.LayoutParams(dp(c,44),dp(c,44));tileWhere.rightMargin=dp(c,14);view.addView(tile,tileWhere);
  LinearLayout words=new LinearLayout(c);words.setOrientation(LinearLayout.VERTICAL);words.setGravity(Gravity.CENTER_VERTICAL);
  TextView name=new TextView(c);name.setText(title);name.setTextColor(skin.text);name.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
  name.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
  words.addView(name,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
  if(sub!=null){
   TextView line=new TextView(c);line.setText(sub.length()==0?" ":sub);line.setTextColor(skin.muted());line.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);
   line.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));line.setSingleLine(true);line.setEllipsize(android.text.TextUtils.TruncateAt.END);
   words.addView(line,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));row.sub=line;
  }
  view.addView(words,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
  if(trailing!=null){
   ImageView end=new ImageView(c);end.setImageDrawable(SettingsGlyph.make(trailing,(skin.text&0xFFFFFF)|0xAA000000));
   LinearLayout.LayoutParams endWhere=new LinearLayout.LayoutParams(dp(c,24),dp(c,24));endWhere.leftMargin=dp(c,8);view.addView(end,endWhere);
  }
  Motion.touch(view);
  if(then!=null)view.setOnClickListener(v->{if(panel!=null)panel.close();try{then.on();}catch(Throwable error){Diary.note("download sheet: "+error);}});
  row.view=view;return row;
 }
 /** The "Best" label on the leading card, a full pill in the accent. */
 private static void pill(Context c,LinearLayout view,String words){
  int accent=Accent.colour();
  TextView mark=new TextView(c);mark.setText(words);mark.setTextColor(onAccent(accent));mark.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);
  mark.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));mark.setGravity(Gravity.CENTER);mark.setSingleLine(true);
  mark.setPadding(dp(c,16),dp(c,9),dp(c,16),dp(c,9));
  GradientDrawable back=new GradientDrawable();back.setCornerRadius(dp(c,24));back.setColor(accent);mark.setBackground(back);
  LinearLayout.LayoutParams where=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);where.leftMargin=dp(c,8);
  view.addView(mark,where);
 }
 /** A plain text button, centred under the cards. */
 private static View cancel(Context c,Skin skin,final Panel panel){
  TextView b=new TextView(c);b.setText(Text.DL_CANCEL);b.setTextColor(skin.text);b.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
  b.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));b.setGravity(Gravity.CENTER);b.setMinHeight(dp(c,52));
  b.setBackground(Expressive.tile(c,0,(Accent.colour()&0xFFFFFF)|0x33000000,dp(c,26),dp(c,26),dp(c,14)));
  b.setClickable(true);b.setFocusable(true);Motion.touch(b);b.setOnClickListener(v->panel.close());
  return b;
 }

 // ------------------------------------------------------------------- sizes

 private static void sizeInto(Row row,DownloadQuality.Choice item,boolean rate){
  String first=item.candidates().isEmpty()?null:item.candidates().get(0);
  sizeInto(row,first,rate?item.bitrate:0L);
 }
 /** Shows the bitrate at once (if any), then the real file size when a HEAD request has found it. */
 private static void sizeInto(final Row row,final String address,final long bitrate){
  if(row.sub==null)return;
  final String fallback=bitrate>0&&bitrate!=Long.MAX_VALUE?Math.round(bitrate/1000.0)+Text.t(" кбит/с"):" ";
  Long known;synchronized(sizes){known=address==null?null:sizes.get(address);}
  if(known!=null){row.sub.setText(format(known));return;}
  row.sub.setText(fallback);
  if(address==null)return;
  Net.away("size",()->{
   long bytes=length(address);if(bytes<=0)return;
   synchronized(sizes){sizes.put(address,bytes);}
   final String text=format(bytes);
   row.view.post(()->{row.sub.setText(text);Motion.change(row.sub);});
  });
 }
 private static String format(long bytes){
  if(bytes>=1024L*1024L)return String.format(java.util.Locale.US,"%.1f %s",bytes/1048576.0,Text.UNIT_MB);
  if(bytes>=1024L)return String.format(java.util.Locale.US,"%.1f %s",bytes/1024.0,Text.UNIT_KB);
  return bytes+" B";
 }
 /** The size of a file in bytes without downloading it, or -1. HEAD first, then a one-byte range. */
 private static long length(String address){
  for(int attempt=0;attempt<2;attempt++){
   String at=address;java.net.HttpURLConnection link=null;
   try{
    for(int hop=0;hop<4;hop++){
     if(!at.startsWith("https://"))break;
     link=(java.net.HttpURLConnection)new java.net.URL(at).openConnection();
     link.setInstanceFollowRedirects(false);link.setConnectTimeout(8000);link.setReadTimeout(8000);
     link.setRequestProperty("User-Agent","ttcuz");link.setRequestProperty("Accept-Encoding","identity");
     if(attempt==0)link.setRequestMethod("HEAD");else link.setRequestProperty("Range","bytes=0-0");
     int code=link.getResponseCode();
     if(code>=300&&code<400){String to=link.getHeaderField("Location");link.disconnect();link=null;if(to==null||to.length()==0)break;at=new java.net.URL(new java.net.URL(at),to).toString();continue;}
     if(code==200&&attempt==0){long n=link.getContentLengthLong();if(n>0)return n;}
     if(code==206||(code==200&&attempt==1)){
      String range=link.getHeaderField("Content-Range");
      if(range!=null){int slash=range.lastIndexOf('/');if(slash>0){try{long n=Long.parseLong(range.substring(slash+1).trim());if(n>0)return n;}catch(NumberFormatException ignored){}}}
      if(code==200){long n=link.getContentLengthLong();if(n>0)return n;}
     }
     break;
    }
   }catch(Throwable ignored){}
   finally{if(link!=null)link.disconnect();}
  }
  return -1;
 }

 /**
  * The photos themselves in a row that scrolls sideways, nothing else: a tap marks one (a round check in its top
  * right corner) and a button under the row saves those marked.
  */
 private static View photoList(final Context c,final Skin skin,final Panel panel,final List<DownloadQuality.Choice> photos,final Pick pick){
  LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);
  LinearLayout list=new LinearLayout(c);list.setOrientation(LinearLayout.HORIZONTAL);
  final boolean[] marked=new boolean[photos.size()];
  final TextView save=new TextView(c);
  final Runnable count=()->{int n=0;for(boolean m:marked)if(m)n++;save.setText(Text.t("Скачать выбранные")+(n>0?" ("+n+")":""));save.setEnabled(n>0);save.setAlpha(n>0?1f:.45f);};
  int radius=dp(c,14);
  for(int i=0;i<photos.size();i++){
   final int index=i;final DownloadQuality.Choice item=photos.get(i);
   FrameLayout frame=new FrameLayout(c);
   GradientDrawable back=new GradientDrawable();back.setCornerRadius(radius);back.setColor((skin.text&0xFFFFFF)|0x14000000);
   // shimmering until its preview arrives; then the plain tile under the picture
   frame.setTag(back);frame.setBackground(Skeleton.block(skin,radius));frame.setClipToOutline(true);
   final View check=checkMark(c,false);
   FrameLayout.LayoutParams corner=new FrameLayout.LayoutParams(dp(c,22),dp(c,22),Gravity.TOP|Gravity.END);
   corner.topMargin=dp(c,6);corner.rightMargin=dp(c,6);
   String first=item.candidates().isEmpty()?null:item.candidates().get(0);
   if(first!=null)preview(frame,first,dp(c,136));
   frame.addView(check,corner);
   frame.setContentDescription(Text.t("Фото")+" "+(i+1));
   frame.setClickable(true);frame.setFocusable(true);Motion.touch(frame);
   frame.setOnClickListener(v->{marked[index]=!marked[index];paintCheck(c,check,marked[index]);v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);count.run();});
   LinearLayout.LayoutParams at=new LinearLayout.LayoutParams(dp(c,104),dp(c,136));
   at.leftMargin=i==0?0:dp(c,6);
   list.addView(frame,at);
  }
  android.widget.HorizontalScrollView scroll=new android.widget.HorizontalScrollView(c);
  scroll.setHorizontalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
  scroll.addView(list);
  box.addView(scroll);
  save.setGravity(Gravity.CENTER);save.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
  save.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  int accent=Accent.colour();save.setTextColor(onAccent(accent));save.setMinHeight(dp(c,50));
  save.setBackground(Expressive.tile(c,accent,0x33FFFFFF,dp(c,25),dp(c,25),dp(c,12)));
  Motion.touch(save);
  save.setOnClickListener(v->{panel.close();for(int k=0;k<marked.length;k++)if(marked[k])pick.on(photos.get(k));});
  count.run();
  box.addView(save,gap(c,10));
  return box;
 }

 /** The round check in a photo's corner: an empty ring, or filled with the accent and ticked. */
 private static View checkMark(Context c,boolean on){View v=new View(c);paintCheck(c,v,on);return v;}
 private static void paintCheck(Context c,View v,boolean on){
  final int accent=Accent.colour();final float d=c.getResources().getDisplayMetrics().density;
  v.setBackground(new android.graphics.drawable.Drawable(){
   final android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
   @Override public void draw(android.graphics.Canvas canvas){
    android.graphics.Rect b=getBounds();float cx=b.exactCenterX(),cy=b.exactCenterY(),r=Math.min(b.width(),b.height())/2f-d;
    p.setStyle(android.graphics.Paint.Style.FILL);p.setColor(on?accent:0x40000000);canvas.drawCircle(cx,cy,r,p);
    p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setColor(0xFFFFFFFF);canvas.drawCircle(cx,cy,r,p);
    if(on){android.graphics.Path tick=new android.graphics.Path();tick.moveTo(cx-r*.42f,cy+r*.02f);tick.lineTo(cx-r*.1f,cy+r*.34f);tick.lineTo(cx+r*.46f,cy-r*.3f);
     p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setStrokeJoin(android.graphics.Paint.Join.ROUND);p.setColor(onAccent(accent));canvas.drawPath(tick,p);}
   }
   @Override public void setAlpha(int a){}
   @Override public void setColorFilter(android.graphics.ColorFilter f){}
   @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
  });
  if(Motion.enabled(c)){v.setScaleX(.8f);v.setScaleY(.8f);v.animate().scaleX(1f).scaleY(1f).setDuration(160L).start();}
 }

 private static final java.util.concurrent.ExecutorService previews=java.util.concurrent.Executors.newFixedThreadPool(2);
 /** Kept by their size in bytes: the large previews of a photo post, a few screens of them at most. */
 private static final android.util.LruCache<String,android.graphics.Bitmap> thumbs=new android.util.LruCache<String,android.graphics.Bitmap>(24*1024*1024){
  @Override protected int sizeOf(String key,android.graphics.Bitmap value){return value.getByteCount();}
 };

 /** The photo itself, small, in place of the row's icon: fetched off the main thread and decoded at the tile's size. */
 /** The tile's skeleton gives way to its plain background once the picture is in (it would shimmer under it). */
 private static void settle(FrameLayout tile){Object plain=tile.getTag();if(plain instanceof android.graphics.drawable.Drawable)tile.setBackground((android.graphics.drawable.Drawable)plain);}

 private static void preview(final FrameLayout tile,final String address){preview(tile,address,dp(tile.getContext(),44));}

 private static void preview(final FrameLayout tile,final String address,final int side){
  final ImageView picture=new ImageView(tile.getContext());
  final String key=address+"#"+side;
  picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
  tile.setClipToOutline(true);
  tile.addView(picture,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
  android.graphics.Bitmap known=thumbs.get(key);
  if(known!=null){picture.setImageBitmap(known);settle(tile);return;}
  previews.execute(()->{
   try{
    java.net.HttpURLConnection link=(java.net.HttpURLConnection)new java.net.URL(address).openConnection();
    link.setConnectTimeout(8000);link.setReadTimeout(12000);
    byte[] bytes;
    try(java.io.InputStream in=link.getInputStream()){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[16384];int n;while((n=in.read(buf))>0&&out.size()<12_000_000)out.write(buf,0,n);bytes=out.toByteArray();}
    finally{link.disconnect();}
    android.graphics.BitmapFactory.Options size=new android.graphics.BitmapFactory.Options();size.inJustDecodeBounds=true;
    android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,size);
    int sample=1;while(Math.min(size.outWidth,size.outHeight)/(sample*2)>=side)sample*=2;
    android.graphics.BitmapFactory.Options read=new android.graphics.BitmapFactory.Options();read.inSampleSize=sample;
    final android.graphics.Bitmap small=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,read);
    if(small==null){tile.post(()->settle(tile));return;}
    thumbs.put(key,small);
    picture.post(()->{picture.setImageBitmap(small);picture.setAlpha(0f);picture.animate().alpha(1f).setDuration(160L).start();settle(tile);});
   }catch(Throwable error){Diary.note("photo preview: "+error);tile.post(()->settle(tile));}
  });
 }
 private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
}
