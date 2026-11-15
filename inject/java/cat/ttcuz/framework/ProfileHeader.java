package cat.ttcuz.framework;

import com.ss.android.ugc.aweme.profile.model.User;
import com.ss.android.ugc.profile.platform.base.data.ProfileComponents;
import com.ss.android.ugc.profile.platform.base.data.ProfileUser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import kotlin.jvm.functions.Function1;

public final class ProfileHeader {
    static final String OLD_GROUP = "profile_old_group";
    private static volatile Method original;

    private ProfileHeader() {
    }

    static boolean oldGroup() {
        try {
            return Ttcuz.context().getSharedPreferences("ttcuz", 0).getBoolean(OLD_GROUP, false);
        } catch (Throwable unused) {
            return false;
        }
    }

    public static ProfileUser LJIILIIL(User user, ProfileUser profileUser, boolean z, Function1 function1) {
        if (profileUser != null) {
            try {
                if (Flags.newProfile()) {
                    if (rearrange(profileUser, z) && !oldGroup()) {
                        Ttcuz.context().getSharedPreferences("ttcuz", 0).edit().putBoolean(OLD_GROUP, true).apply();
                    }
                } else {
                    classic(profileUser);
                }
            } catch (Throwable th) {
                Diary.note("profile header: " + String.valueOf(th));
            }
        }
        try {
            Method method = original;
            if (method == null) {
                method = Class.forName("X.0Oby", true, ProfileHeader.class.getClassLoader()).getDeclaredMethod("LJIILIIL", User.class, ProfileUser.class, Boolean.TYPE, Function1.class);
                method.setAccessible(true);
                original = method;
            }
            return (ProfileUser) method.invoke(null, user, profileUser, Boolean.valueOf(z), function1);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw ((RuntimeException) cause);
            }
            if (cause instanceof Error) {
                throw ((Error) cause);
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e2) {
            Diary.note("profile header: TikTok's method is gone, " + String.valueOf(e2));
            return profileUser;
        }
    }

    static boolean rearrange(ProfileUser profileUser, boolean z) {
        ProfileComponents profileComponents = profileUser.headerComponents;
        if (profileComponents != null && profileComponents.components != null) {
            ArrayList arrayList = new ArrayList(profileComponents.components);
            ProfileComponents named = named(arrayList, "header_avatar");
            ProfileComponents named2 = named(arrayList, "info");
            if (named != null && named2 != null && named2.components != null) {
                ArrayList<ProfileComponents> arrayList2 = new ArrayList(named2.components);
                if (named(arrayList2, "user_basic_info") != null) {
                    return false;
                }
                ProfileComponents named3 = named(arrayList2, "user_info");
                if (named3 == null) {
                    ArrayList arrayList3 = new ArrayList();
                    for (ProfileComponents profileComponents2 : arrayList2) {
                        if (profileComponents2 != null && ("user_account_name_info".equals(profileComponents2.componentName) || "user_account_base_info".equals(profileComponents2.componentName))) {
                            arrayList3.add(profileComponents2);
                        }
                    }
                    if (arrayList3.isEmpty()) {
                        return false;
                    }
                    ProfileComponents part = part("user_info", arrayList3);
                    arrayList2.add(arrayList2.indexOf(arrayList3.get(0)), part);
                    arrayList2.removeAll(arrayList3);
                    named3 = part;
                }
                ArrayList arrayList4 = named3.components == null ? new ArrayList() : new ArrayList(named3.components);
                ProfileComponents named4 = named(arrayList2, "user_relation_info");
                if (named4 != null) {
                    arrayList2.remove(named4);
                    if (named(arrayList4, "user_relation_info") == null) {
                        arrayList4.add(named4);
                    }
                }
                named3.components = arrayList4;
                ArrayList arrayList5 = new ArrayList(3);
                if (!z) {
                    arrayList5.add(part("", null));
                }
                arrayList5.add(named3);
                arrayList5.add(named);
                arrayList2.set(arrayList2.indexOf(named3), part("user_basic_info", arrayList5));
                named2.components = arrayList2;
                arrayList.remove(named);
                profileComponents.components = arrayList;
                return true;
            }
        }
        return false;
    }

    static boolean classic(ProfileUser profileUser) {
        ProfileComponents named;
        ProfileComponents named2;
        ProfileComponents profileComponents = profileUser.headerComponents;
        if (profileComponents != null && profileComponents.components != null && (named = named(profileComponents.components, "info")) != null && named.components != null && (named2 = named(named.components, "user_basic_info")) != null && named2.components != null) {
            ProfileComponents named3 = named(named2.components, "header_avatar");
            ProfileComponents named4 = named(named2.components, "user_info");
            if (named3 != null && named4 != null) {
                ArrayList arrayList = new ArrayList(profileComponents.components);
                ArrayList arrayList2 = new ArrayList(named.components);
                int indexOf = arrayList2.indexOf(named2);
                arrayList2.remove(named2);
                arrayList2.add(indexOf, named4);
                for (ProfileComponents profileComponents2 : named2.components) {
                    if (profileComponents2 != null && profileComponents2 != named4 && profileComponents2 != named3 && profileComponents2.componentName != null && !profileComponents2.componentName.isEmpty()) {
                        indexOf++;
                        arrayList2.add(indexOf, profileComponents2);
                    }
                }
                if (named4.components != null) {
                    ArrayList arrayList3 = new ArrayList(named4.components);
                    ProfileComponents named5 = named(arrayList3, "user_relation_info");
                    if (named5 != null) {
                        arrayList3.remove(named5);
                        if (named(arrayList2, "user_relation_info") == null) {
                            arrayList2.add(indexOf + 1, named5);
                        }
                    }
                    named4.components = arrayList3;
                }
                if (named(arrayList, "header_avatar") == null) {
                    arrayList.add(0, named3);
                }
                named.components = arrayList2;
                profileComponents.components = arrayList;
                return true;
            }
        }
        return false;
    }

    private static ProfileComponents part(String str, List<ProfileComponents> list) {
        ProfileComponents profileComponents = new ProfileComponents();
        profileComponents.componentId = str;
        profileComponents.componentName = str;
        profileComponents.components = list;
        return profileComponents;
    }

    private static ProfileComponents named(List<? extends ProfileComponents> list, String str) {
        for (ProfileComponents profileComponents : list) {
            if (profileComponents != null && str.equals(profileComponents.componentName)) {
                return profileComponents;
            }
        }
        return null;
    }
}
