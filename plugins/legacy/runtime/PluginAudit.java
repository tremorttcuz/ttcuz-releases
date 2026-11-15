package cat.narezany.margyt;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.Adler32;

/** Package integrity only. No API, capability or obfuscation policy. */
public final class PluginAudit {
    private PluginAudit() {}
    public static final int MAX_DEX=16*1024*1024;
    public static final class Report {
        public final Set<String> findings=new LinkedHashSet<String>();
        public boolean blocked;
        public String digest="";
        public String pluginId="";
        public String summary() {
            return "Пакет прочитан. Ограничения API и обфускации отключены. После установки плагин выключен.";
        }
    }
    public static byte[] read(File file,int limit)throws IOException{
        try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;
            while((n=in.read(b))!=-1){total+=n;if(total>limit)throw new IOException("Превышен размер файла плагина");out.write(b,0,n);}return out.toByteArray();}
    }
    public static String sha256(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return b.toString();}
    public static Report folder(File folder)throws Exception{
        byte[] dex=read(new File(folder,"classes.dex"),MAX_DEX);
        Report report=dex(dex);
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        for(String name:new String[]{"manifest.json","classes.dex","icon.png"}){
            hash.update(name.getBytes("UTF-8"));File file=new File(folder,name);
            if(file.isFile())hash.update(MessageDigest.getInstance("SHA-256").digest(read(file,name.equals("classes.dex")?MAX_DEX:name.equals("icon.png")?1024*1024:65536)));
        }
        report.digest=hex(hash.digest());return report;
    }
    public static Report dex(byte[] data) throws Exception {
        new Dex(data);
        Report report = new Report();
        report.digest = sha256(data);
        return report;
    }

    private static final class Dex {
        final byte[] b;private final Map<Integer,String> decoded=new HashMap<Integer,String>();private int textBytes,ulebReads;final int stringCount,stringOff,typeCount,typeOff,methodCount,methodOff;
        Dex(byte[] bytes)throws Exception{
            b=bytes;
            if(b.length<112||b.length>MAX_DEX||b[0]!='d'||b[1]!='e'||b[2]!='x'||b[3]!=10||b[7]!=0)throw new IOException("Некорректный DEX");
            String version=new String(b,4,3,"US-ASCII");if(!Arrays.asList("035","037","038","039","040").contains(version))throw new IOException("Неподдерживаемая версия DEX");
            if(u32(32)!=b.length||u32(36)!=112||u32(40)!=0x12345678)throw new IOException("Некорректный заголовок DEX");
            MessageDigest sha=MessageDigest.getInstance("SHA-1");sha.update(b,32,b.length-32);
            if(!Arrays.equals(sha.digest(),Arrays.copyOfRange(b,12,32)))throw new IOException("Повреждён DEX: SHA-1");
            Adler32 checksum=new Adler32();checksum.update(b,12,b.length-12);
            if(checksum.getValue()!=(raw32(8)&0xffffffffL))throw new IOException("Повреждён DEX: checksum");
            stringCount=u32(56);stringOff=u32(60);typeCount=u32(64);typeOff=u32(68);methodCount=u32(88);methodOff=u32(92);
            table(stringOff,stringCount,4);table(typeOff,typeCount,4);table(methodOff,methodCount,8);
            // Ensure all string entries are bounded, including unused entries.
            for(int i=0;i<stringCount;i++)string(i);
        }
        int raw32(int p)throws IOException{range(p,4);return (b[p]&255)|((b[p+1]&255)<<8)|((b[p+2]&255)<<16)|((b[p+3]&255)<<24);}
        int u32(int p)throws IOException{int value=raw32(p);if(value<0)throw new IOException("Слишком большое значение DEX");return value;}
        int u16(int p)throws IOException{range(p,2);return (b[p]&255)|((b[p+1]&255)<<8);}
        void range(int p,long n)throws IOException{if(p<0||n<0||(long)p+n>b.length)throw new IOException("Таблица за границами DEX");}
        void table(int off,int count,int size)throws IOException{if(count>100000||(count==0)!=(off==0))throw new IOException("Некорректная таблица DEX");range(off,(long)count*size);}
        int uleb(int[] pos)throws IOException{if(++ulebReads>1000000)throw new IOException("Слишком сложный DEX для проверки");long value=0;for(int i=0;i<5;i++){range(pos[0],1);int v=b[pos[0]++]&255;value|=(long)(v&127)<<(i*7);if((v&128)==0){if(value>Integer.MAX_VALUE)throw new IOException("Переполнение DEX");return (int)value;}}throw new IOException("Некорректный ULEB128");}
        String string(int index)throws IOException{
            if(index<0||index>=stringCount)throw new IOException("Индекс строки DEX");
            int offset=u32(stringOff+index*4);String ready=decoded.get(offset);if(ready!=null)return ready;
            int[] pos={offset};uleb(pos);int start=pos[0];
            while(true){range(pos[0],1);if(b[pos[0]++]==0)break;if(++textBytes>MAX_DEX||pos[0]-start>65536)throw new IOException("Слишком большие строки DEX");}
            ready=new String(b,start,pos[0]-start-1,java.nio.charset.StandardCharsets.UTF_8);decoded.put(offset,ready);return ready;
        }
        String type(int index)throws IOException{if(index<0||index>=typeCount)throw new IOException("Индекс типа DEX");return string(u32(typeOff+index*4));}
    }
}
