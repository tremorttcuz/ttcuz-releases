"""Execute database selection and fresh-state checks, including unloaded request chats."""
import unittest
import test_settings_safety_runtime as safety


class InboxReadRuntimeTest(unittest.TestCase):
    run_java = safety.SafetyRuntimeTest.run_java

    def test_native_cell_click_starts_action(self):
        methods = '\n'.join(safety.method('InboxReadAll.java', marker) for marker in (
            '    public static void addMenu(', '    private static void bindClick(',
            '    private static Object objectMethod('))
        methods = methods.replace('X.0lnT', 'Test$Cell').replace('X.0GtW', 'Test$Icon')
        methods = methods.replace('InboxReadAll::start', 'Test::start')
        self.run_java({
            'android/content/Context.java': 'package android.content;public class Context{}',
            'android/util/AttributeSet.java': 'package android.util;public interface AttributeSet{}',
            'android/view/View.java': '''package android.view;public class View {
 public interface OnClickListener {void onClick(View v);}
 public void setOnClickListener(OnClickListener l){}
 public void setContentDescription(CharSequence s){}
}''',
            'android/view/ViewGroup.java': '''package android.view;public class ViewGroup extends View {
 public Object tag;public View row;public int additions;
 public android.content.Context getContext(){return new android.content.Context();}
 public Object getTag(int key){return tag;}public void setTag(int key,Object value){tag=value;}
 public void addView(View v,LayoutParams p){row=v;additions++;}
 public static class LayoutParams{public LayoutParams(int w,int h){}}
}''',
            'kotlin/jvm/functions/Function1.java': 'package kotlin.jvm.functions;public interface Function1 {Object invoke(Object value);}',
            # Exact singleton field in official 47.2.41, not the unminified Kotlin SDK.
            'kotlin/Unit.java': 'package kotlin;public class Unit{public static final Unit LIZ=new Unit();}',
            'Test.java': '''import android.content.Context;import android.view.*;import java.lang.reflect.*;
public class Test {
 static final int TAG=1;static int calls;static void start(){calls++;}
 static class Diary{static void note(String s){throw new AssertionError(s);}}
 public static class Icon{public int LIZ;public Integer LJ;}
 public static class Cell extends View {
  kotlin.jvm.functions.Function1 listener;
  public Cell(Context c,android.util.AttributeSet a,int style){}
  public void setTitle(CharSequence s){}public void setIcon(Icon i){}
  @Override public void setOnClickListener(OnClickListener ignored){}
  public void setOnClickListener(kotlin.jvm.functions.Function1 l){listener=l;}
  Object tap(){return listener==null?null:listener.invoke(Boolean.TRUE);}
 }
''' + methods + '''
 public static void main(String[] args){
  ViewGroup menu=new ViewGroup();addMenu(menu);addMenu(menu);
  if(menu.additions!=1)throw new AssertionError("duplicate menu item");
  Cell row=(Cell)menu.row;
  if(calls!=0)throw new AssertionError("ran before click");
  if(row.tap()!=kotlin.Unit.LIZ||calls!=1)throw new AssertionError("tap did not start read-all");
  if(row.tap()!=kotlin.Unit.LIZ||calls!=2)throw new AssertionError("second tap lost");
  if(!row.listener.equals(row.listener)||row.listener.equals(new Object()))throw new AssertionError("callback identity");
  row.listener.hashCode();row.listener.toString();
 }
}'''
        })

    def test_native_notice_payload_and_acknowledged_clear(self):
        methods = '\n'.join(safety.method('InboxReadAll.java', marker) for marker in (
            '    private static Object noticeRequest(', '    private static void readNotices(',
            '    private static String resultMessage('))
        replacements = {'X.0mFh':'Test$Experiments', 'X.09BV':'Test$Flag',
            'X.0mG6':'Test$Params', 'X.01SE':'Test$Lazy', 'X.05C0':'Test$Business',
            'X.1Gjy':'Test$Manager', 'X.1Exe':'Test$Occasion',
            'com.ss.android.ugc.aweme.inbox.widget.multi.MultiViewModel':'Test$Model',
            'com.ss.android.ugc.aweme.inbox.api.MultiApiManager$API':'Test$API',
            'com.ss.android.ugc.aweme.inbox.api.MultiApiManager':'Test$Factory'}
        for old, new in replacements.items(): methods = methods.replace(old, new)
        self.run_java({'Test.java': '''import java.util.*;
public class Test {
 public static class Experiments {public static Experiments LL=new Experiments();public static String LIZLLL(){return "cached";}public String LIZJ(){return "live";}}
 public static class Flag {static boolean cached;public static boolean LIZ(){return cached;}}
 public static class Params {public static String LIZ(String s){return "native:"+s;}}
 public interface Lazy {Object getValue();}
 public static class Model {public static Lazy LLLLLLZZ=()->"native-groups";}
 public static class Business {public static String LIZ(){return "native-business";}}
 public static class Factory {static API api=new API();public static API LIZ(){return api;}}
 public static class API {String experiment;public Object LIZIZ(String e,String g,int scenario,Boolean read,Boolean time,Boolean unread,String biz){
  if(!g.equals("native-groups")||!biz.equals("native-business")||scenario!=1||!read||time||unread)throw new AssertionError("non-stock request parameters");experiment=e;return new Object();}}
 public static class Occasion {}
 public static class Manager {
  public static Manager LL=new Manager();static Map<Integer,Integer> attrs=new HashMap<>(),counts=new HashMap<>();static Set<Integer> cleared=new HashSet<>();
  public static Map<Integer,Integer> LJJJJ(){return attrs;}public static Map<Integer,Integer> LJJJJI(){return counts;}
  public void LJFF(Occasion o,boolean force,int[] groups){if(o!=null||!force)throw new AssertionError();for(int g:groups)cleared.add(g);}}
 static class NativeRead {static Object field(Object o,String n){return status;}}
 static int status;static boolean networkError;static Object await(Object request){if(networkError)throw new IllegalStateException("offline");return new Object();}
 static void checkAccount(String uid){if(!uid.equals("123"))throw new IllegalStateException();}
 interface Task{void run()throws Exception;}static void onMain(Task t)throws Exception{t.run();}
''' + methods + '''
 public static void main(String[] args)throws Exception {
  Manager.attrs.put(1,1);Manager.counts.put(2,4);readNotices("123");
  if(!Factory.api.experiment.equals("native:live")||!Manager.cleared.equals(new HashSet<>(Arrays.asList(1,2))))throw new AssertionError("activity/followers omitted");
  Flag.cached=true;noticeRequest();if(!Factory.api.experiment.equals("native:cached"))throw new AssertionError("cached experiment branch lost");
  Manager.cleared.clear();status=5;try{readNotices("123");throw new AssertionError("rejection accepted");}catch(IllegalStateException expected){}
  if(!Manager.cleared.isEmpty())throw new AssertionError("cleared before acknowledgement");status=0;networkError=true;
  try{readNotices("123");throw new AssertionError("network failure accepted");}catch(IllegalStateException expected){}
  if(!Manager.cleared.isEmpty())throw new AssertionError("network failure erased counters");
  if(!resultMessage(true,false).contains("Активность прочитать не удалось")||!resultMessage(false,true).contains("Часть сообщений"))throw new AssertionError("partial success hidden");
 }
}'''})

    def test_all_categories_and_replaced_conversation_objects(self):
        methods = '\n'.join(safety.method('InboxReadAll.java', marker) for marker in (
            '    private static List<Object> unread(', '    private static long count(',
            '    private static boolean stillUnread(', '    private static void checkAccount('))
        for old, new in {'X.0Rr5': 'Test$Filter', 'X.0RxE': 'Test$Order',
                         'X.06Yr': 'Test$Cursor', 'X.06kl': 'Test$Query'}.items():
            methods = methods.replace(old, new)
        self.run_java({'Test.java': '''import java.util.*;
public class Test {
 public static class Filter {public Filter(){}}
 public static class Order {public static Order SORT_ORDER_DESC=new Order();}
 public static class Cursor {}
 public static class Query {public Query(List<?> f,Order o,int limit,Cursor c,boolean stranger,String seq){
  if(f.size()!=1||!(f.get(0) instanceof Filter)||o!=Order.SORT_ORDER_DESC||limit!=-1||c!=null||!stranger)throw new AssertionError("incomplete database query");}}
 public static class Result {public Page LIZ;boolean error;Result(List<?> values){LIZ=new Page(values);}public boolean LIZJ(){return error;}}
 public static class Page {public List<?> LIZIZ;Page(List<?> values){LIZIZ=values;}}
 public static class Conversation {String id;long count;Conversation(String id,long n){this.id=id;count=n;}public long getUnreadCount(){return count;}public String getConversationId(){return id;}}
 public static class Model {Result result;Map<String,Conversation> current=new HashMap<>();public Result LJIIJ(Query q){return result;}public Conversation LIZ(String id){return current.get(id);}}
 static class NativeRead {static Object field(Object o,String f){try{return o.getClass().getField(f).get(o);}catch(Exception e){return null;}}}
 static class Account {static String uid="123";static String liveId(){return uid;}}
''' + methods + '''
 public static void main(String[] args)throws Exception {
  Model m=new Model();Conversation single=new Conversation("single",12),group=new Conversation("group",3),request=new Conversation("unloaded request",1),read=new Conversation("read",0);
  m.result=new Result(Arrays.asList(single,group,request,read));List<Object> all=unread(m);
  if(all.size()!=3||!all.contains(single)||!all.contains(request))throw new AssertionError("personal/request chats omitted");
  if(!stillUnread(m,all))throw new AssertionError("unread input lost");
  for(Object v:all){Conversation c=(Conversation)v;m.current.put(c.id,new Conversation(c.id,0));}
  if(stillUnread(m,all))throw new AssertionError("stale object used after SDK replacement");
  m.result.error=true;try{unread(m);throw new AssertionError("query failure treated as success");}catch(IllegalStateException expected){}
  checkAccount("123");Account.uid="456";try{checkAccount("123");throw new AssertionError("account swap ignored");}catch(IllegalStateException expected){}
 }
}'''})
