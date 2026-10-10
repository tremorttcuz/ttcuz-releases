package cat.narezany.margyt;
/** Coalesce unchanged payloads and pause after failed publication. */
final class SyncGate {
    private String sent,queued;private int failures;private long notBefore,manualAt;
    synchronized boolean queue(String signature) {
        if(signature.equals(sent) || signature.equals(queued))return false;
        queued=signature;return true;
    }
    synchronized long delay(long now){return Math.max(800L,notBefore-now);}
    synchronized void complete(String signature,boolean okay,long now) {
        if(signature.equals(queued))queued=null;
        if(okay){sent=signature;failures=0;notBefore=0;}
        else {failures=Math.min(6,failures+1);notBefore=now+Math.min(600000L,30000L<<(failures-1));}
    }
    synchronized boolean canRetry(long now){return queued==null && (manualAt==0 || now-manualAt>=10000L);}
    synchronized boolean retry(long now) {
        if(!canRetry(now))return false;
        sent=null;failures=0;notBefore=0;manualAt=now;return true;
    }
    synchronized void reset(){sent=queued=null;failures=0;notBefore=0;}
}
