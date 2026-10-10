package cat.narezany.margyt;
/** Admin controls depend on the live session, never on a nickname or cached badge. */
final class AdminAccess {
    private AdminAccess(){}
    static boolean allowed(String liveUid){return "7491898648855512119".equals(liveUid);}
    static boolean current(){return allowed(Account.activeId());}
}
