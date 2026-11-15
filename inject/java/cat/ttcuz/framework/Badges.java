package cat.ttcuz.framework;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
import cat.ttcuz.framework.Net;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public final class Badges {
    private static final Badge CREATOR_PLACEHOLDER;
    private static final String CREATOR_UID = "7491898648855512119";
    // every three minutes, and only while TikTok is on screen: the server is on a free plan with a daily allowance
    private static final long EVERY = 3 * 60 * 1000L;
    private static final String FILES = Server.ROOT + "/";
    private static final char FIRST;
    public static final String KEY = "badges_on";
    private static final String MARGYT = "https://margy.narez.xyz/api/";
    private static final String MARGYT_IMAGES = "margyt:";
    public static final String MARGYT_KEY = "badges_margyt_on";
    private static final int MOST = 256;
    public static final String SERVER = "";
    private static final String SOURCE = Server.ROOT + "/badges";
    private static volatile Boolean cached;
    private static volatile long freeUntil;
    private static volatile Map<String, Badge[]> known;
    private static Map<String, Badge[]> margytBadges;
    private static Badge[] margytOrder;
    private static volatile String margytTag;
    private static volatile Badge[] numbered;
    private static Map<String, Badge[]> ownBadges;
    private static volatile Bitmap ownNote;
    private static Badge[] ownOrder;
    private static final Map<String, Long> pictureAsked;
    private static final Map<String, Bitmap> pictures;
    private static volatile long readAt;
    private static volatile long serverNow;
    private static volatile boolean started;
    private static volatile String tag;
    private static Map<String, Badge[]> ttyBadges;
    private static Badge[] ttyOrder;
    private static volatile Runnable waiting;

    public static boolean isPrivate(char c) {
        return c >= 57344 && c <= 63743;
    }

    private Badges() {
    }

    public static final class Badge {
        public final String button;
        public final int colour;
        public final String id;
        public final String image;
        public final String text;
        public final String title;

        Badge(String str, String str2, int i, String str3, String str4, String str5) {
            this.id = str;
            this.image = str2;
            this.colour = i;
            this.title = str3;
            this.text = str4;
            this.button = str5;
        }
    }

    public static boolean margytEnabled() {
        try {
            Context context = Ttcuz.context();
            if (context != null) {
                return context.getSharedPreferences("ttcuz", 0).getBoolean(MARGYT_KEY, true);
            }
            return true;
        } catch (RuntimeException unused) {
            return true;
        }
    }

    public static void setMargytEnabled(boolean z) {
        Context context = Ttcuz.context();
        if (context == null) {
            return;
        }
        context.getSharedPreferences("ttcuz", 0).edit().putBoolean(MARGYT_KEY, z).apply();
        cat.ttcuz.framework.Badge.refreshProfiles();
    }

    public static boolean isEnabled() {
        Boolean bool = cached;
        if (bool != null) {
            return bool.booleanValue();
        }
        try {
            Context context = Ttcuz.context();
            if (context == null) {
                return true;
            }
            boolean z = context.getSharedPreferences("ttcuz", 0).getBoolean(KEY, true);
            cached = Boolean.valueOf(z);
            return z;
        } catch (Throwable unused) {
            return true;
        }
    }

    public static void setEnabled(boolean z) {
        cached = Boolean.valueOf(z);
        try {
            Context context = Ttcuz.context();
            if (context == null) {
                return;
            }
            context.getSharedPreferences("ttcuz", 0).edit().putBoolean(KEY, z).apply();
        } catch (Throwable unused) {
        }
    }

    static {
        Badge badge = new Badge("creator_trem0r1_1", "", 0, "@trem0r1_1", Text.t("Создатель ttcuz"), Text.t("Профиль TikTok"));
        CREATOR_PLACEHOLDER = badge;
        known = initialBadges();
        ownBadges = initialBadges();
        margytBadges = new HashMap();
        ownOrder = new Badge[]{badge};
        margytOrder = new Badge[0];
        ttyBadges = new HashMap();
        ttyOrder = new Badge[0];
        numbered = new Badge[]{badge};
        FIRST = pick();
        pictures = new HashMap();
        pictureAsked = new HashMap();
    }

    static synchronized void applyTty(Map<String, Badge[]> map, Badge badge) {
        synchronized (Badges.class) {
            ttyBadges = map;
            ttyOrder = new Badge[]{badge};
            mergeFeeds();
            cat.ttcuz.framework.Badge.refreshProfiles();
        }
    }

    static boolean isCreator(Badge badge) {
        return badge != null && "creator_trem0r1_1".equals(badge.id);
    }

    static boolean hasCreatorBadge(String str) {
        return CREATOR_UID.equals(str);
    }

    private static Map<String, Badge[]> initialBadges() {
        HashMap hashMap = new HashMap();
        hashMap.put(CREATOR_UID, new Badge[]{CREATOR_PLACEHOLDER});
        return hashMap;
    }

    private static char pick() {
        return (char) (((int) (Math.random() * 5887)) + 57600);
    }

    public static Bitmap note() {
        Bitmap bitmap = ownNote;
        if (bitmap != null) {
            return bitmap;
        }
        try {
            byte[] decode = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAbAAAAGwCAYAAADITjAqAABTiklEQVR42u2debgU1Zn/s8ddk5hEzaIT11EUERQ3XAGBgIqggrggKC6oKLgCigLihrKo6DXGNS7RaNxHjUtwj+ISk5hkMpPZN2f5y//r15/zm3Pn3KKqu6q6llNd3+/zvM+9vdy+3dXV59Pf97znPZ/7nCRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJkiRJUtO0y+c///mJrZjVir4i4gtf+EJX8cUvfjFTfOlLX0oVX/7ylxPHV77ylUTx1a9+NVFssMEGHWPDDTdsGxtttFHb2HjjjdvGJpts0jY23XTT2Nhss83axuabbx4bW2yxRWx87Wtfi42vf/3rkfGNb3wjNrbccsvI+OY3vxkb3/rWtyLj29/+dmxstdVWkbH11ltHxjbbbBMZ3/nOdyLju9/9bmR873vfI2a1YmIrdtHwJvWaRrRiaSvWtuKzVgQtyBQWLQB1HS0YZYoWlFJFC0yJogWmxNGCU8dowaljtADVNlqA6hgtSLWNFqRiowWpttGCVGy0IBUbLUjFRgtSsdECVWy0QBUZLVDFRgtUkdECVWy0QBUZLVBFRgtUkdECVWy0YBUZLVhFRgta4fisFWu///3vL23FCA1/Uh01tBV9rfgUYLkheAlegld18GoHsBzgZaIFLjc+bUVfK4ZqWJR817RWrAtDS/ASvASvnndeUfDqj2233ZZY14ppGiYlH8H1SRy4BC/BS/BqPLzc+EQgk3xJFa5rBy7BS/ASvBqXNmwHLxPbbbcdsa4VSi1KpWuj/y3MCAQvwUvwkvPKAC83lrZiIw2rUhnapVO6UPASvAQvwSshvEz8xV/8xSetUBm+VKgmfu5/S+EFL8FL8FLaMCd42fisFRM1zEpF6Pwk4BK8BC/BS84rA7zcOF/DrZSn+gQvwUvwErxKgFfwgx/8gOjTsCsJXoJXqfBqBy7BS2nDhPASxCSlDQUvwUvOq7bwsqF0opS5YEPwErwSwcuFkAWHhQG/c52FkeAleCWEV7D99tsTKuyQUmkXVRsKXu3ghaNiEP7Lv/zLYNy4ccGcOXOCZcuWBT/60Y+Chx9+OHj66aeD559/3sRTTz0VPPDAA8Ftt90WLFq0KJg1a1YwYcKEYPfddzcDNiASvJQ2jIEX8VkrVGIvJRILCrXOS/AaAC+AxUA+bNgwA6t77703eOutt4K//uu/Dv7pn/4p+Jd/+ZfgX//1XyPj3/7t39YLrv/bv/3b4M033wzuuuuu4IwzzggGDRpkINIOWoJXM5xXCGDEJ63QYmepo9RhQ/DqBxdAGDJkSHDRRRcFL774YvB3f/d3wT//8z+bAFpR4IoCFvHv//7vkWFvB2iPPvpocPbZZweDBw+OhZng1Yi04Xqxww47LNXwLLXTUMFL8CIYvEkNkvqzLisMrjiAtQNZHMRcmPG3f/zjH0068rDDDjPPRfBqXNowDC8b6p0oxUqNeRsOLyAxfvz44IknnjCOyAVXFLw6uTDXYaUBmf07HB9zasAUqMh5NSptGAWwdRqmpShNE7yaCy8qAXE7pPD+/Oc/rweutBALwyoKYP/xH//RHy643Ovtbf/4j/8Y/OQnPwn2339/Ax7Bq3HOyw1txSKtJ+3n1VB4MUBdc801wZ/+9CcDCuBlI63zSjsHFoZVu+D+uMLFixebwbAduJQ27Fl4BTvuuOMnGq6lxO5L8OpNeFFZSLpw7dq1JlUXhleS9GGa+a92qcM0EONx3njjjWDixIkGOnJePZ82DAOMkAuTOs99CV69CS8G3yVLlphiib//+78P/uEf/sEALM6BFe3E0roxgueMG2MgF7wa4bzc0FyY1L7yUPDqTXix+PiRRx4xrgsIuPDqBDCf3BjB4zBvt+eee7YFl9KGPQUvG6pIlKKb9QpevQcv1nWNGDEieO211/pdVxhg4RRiHSDG/T/88MNg0qRJBjpVwSsOXHJexQBsp512UrNf6XOfCl69Dy/mu5gz+uCDD4zzAmDh1GE7iMWBLE1KMc8Cj08//XQ9iP3N3/xNcNZZZxnIKG3Y084LeBGfavhutkYIXs2A17Rp04JPPvlkALziAJYWYlW5sSiI8RwvvfRSAxSlDXsaXjZGaBhvrpYKXr0Pr1NOOSX47W9/a9Z3RQEsah4sr5Ri2QUeds3YvHnzDFjkvHoubRgOtZdqsNYKXr0LL7q5n3766cZ5WXjZiAJYHm6sqpRilBO74oorzOAvePWk87KxVsN4c/WZ4NWb8KIlFE1x//CHPxh4JQVYnVOK4eCxr7vuOjPYK23Yk/AKdt555880jDdTuwhevQkv5n4WLlxo4EXninYAKxJkebixbiHGY9AUmMFXzqtn0oYuwAjtFdY0tQAzUfDqPXgxkN1yyy2mg7wLLxdiYXjV1Y2lgRh9FBlQBa+ecV5uaMfmBgJsluDVO/CiWGO//fYzOyHT0xB4hQHmOrA4kEVBrKgCjzJTivz9M888ExxwwAEGUEob9gy8iFka0ZsHsD7BqzfgxaDLhpMfffSRWQtFWIDFpRDbQcznlGI3boz7/f73vzdzgwBBzqvWaUM3tKBZABO86gYvOmsMHTo0eOihh0xPQ5xXGGBRKcQqIeaDG+P/3XfffcGwYcMMmASv2jovE7vssosAJoAJXnWBF+BigLnkkkuC999/38x3ufBqB7C0ICszpVjmmjHux9q4OXPmmMFXacPawksAE8AEr7rAi0Fw9uzZweuvv25cl4WXjagUYhzIlFL8d/O4r776ajB16lQDBjmv2qQNBTABTPCqA7xYkLzHHnuY7hIvvviiKY+38EoKMF8hVnZKMXy9vczzeOqpp4KZM2eagREYCV7eOy8BTAATvHyDF06Lfa123XVX00njgQceMAUaYXC1A1gaJ1YmyHxIKba7nsfhWLMU4cgjjzSDMRBS2tBbeAlgApjglQZeYUhZ6Gy66abGLdEFg6C0nfkqbo8DF7fzNxQR8GFk0LzqqquCJ554wsxv0QbKgsuNKICF4SU3Fg2yJGlG68ree++94K677grOPffc4NBDDzXvEdAAQEmclwskfm8HLqUNBTCpBIA1FV7AaosttjAbQrI1CQUA11xzTXD77bcH999/f/DYY48FTz75pFmLRTqKjRapduvr6wtuuummYOnSpcGVV15pdhGmxRHf8u+5557g8ccfD1555RUDLAoLgBZByTcRBbCo+a92IEsKsaYUeCSdK7P34zF5Hb/73e+CtWvXmi8Y9957b3DbbbcFK1euDJYvX26C39esWRPcfffdwU9/+lPz3nIucF787Gc/M+/3qlWrgvnz5wennXaagSJOG7jIeaUGlwAmgAle7aCFk9prr71MKm/16tVmMPrVr35l0ky//vWvg48//ni9+M1vftM2gBTBYGgjDC03AFgYYnEpxLq5MZ8727fbwoWf4e1c3Pu48Z//+Z+xwfPnPfzlL39pYMh5xqajACEOXILXwGh9oRTABDDBywbpQIomWBzMN+d169b1A8uFVhJIhSMMLDfawSsMsXbzYEop5g+yMJTCcGoHq//6r/8aEP/93/8dG9yf1/3GG28EixYtMg4NcChtGAsvAUwAE7wI5qwOP/zw4I477gjeeecdA60ocHVyW+3AFeW62oEsyn3FQaxbkKnAIxnEXDilAVcYYP/zP//TH1Ew474cl1/84hdm6QQDtpzXevASwASwZsMLcI0aNcrMVzFp/+GHH64HrziIpXFgSSEW5bzKgpjWjKUDmAuydhCLc1xxEAtfx+OTCWCXgcGDBwte/wcvAUwAaya8qARkfosii3fffTf44IMPBsArCmKdABaXOkwCsSQpxG5A1i3EkjYE7rWUYqf0YbcQiwJW3PU8Dufh+eefbwb5BqcNBTABrLnwomz94osvDt58800DLhsArB3E8kwjZp0Ha4ob8ymlmBRindKHSdxYO5dmb+d/vPTSS8H48eMNmBrqvAQwAaxZ8KIUfvjw4cEjjzxiytbD4IoCWKc0YhFzYYKYnynFsiCWJLg/x4clGoMGDWoqvFiGIIAJYL0PL+a6Tj311OC1114bAK9O7ispxNKkFH12Y2WtGev1lGLSYo48QEYZ/hFHHNGktKGFlwAmgPU+vFjPdfnll5u5LibDAZiNTg4sDmJKKcqN+eTGeC/p5QicGuK8BDABrPfhRQ+7G2+8MRJe7dKI3bixolOK7daINRFiRbqxbtaHlQkxguPDNjvApSHwEsAEsN6FF2XG9LKjPJ5oB7Ak82FKKZbbhkopxXQg4748pxtuuMHAoYfThgKYANbb8CKF8uCDDxrnFQevKIDVzY0ppaiUYhhi/G96dLoQ60HnJYA1HWC9nDbEeVl4uQ4siQvLG2JyYyrwKNuN8fg0FwYuPQyvYLfddhPAmgiwXi7YuPbaa03T3TiAJYVY0QUegpjWjBUJMZ7PpZdeaqDUY2lDCy8BrIlqwaevF+FFI14WKL/99tsGXjaiANarKUUVePibUiy7wMOuFWP5SA86LwFMAOutRcpTpkwxnbyj3FccxIp2Y0opqsCjSjfGfXjvx44d6yW84sCVEF4CmADWG+2hhgwZYjp300neAqwbiCV1Y1ozpjZUPkPM9lBksTOfEZ/ShjnASwATwOoPry233NLskGvh5QKsDIiV4cbyTClGgUxrxurtxpJA7NZbbzWQ6RHnJYAJYL2xJcqMGTOCt956ywAs7MDyAJkKPFTgUVc35t7G8Zg8eXLd57wGxKBBgwQwAay+8CKv/9xzz/XDK86FKaWoNWNFFHhk2SyzzDVj4dtefvllM/DXsNowCl4CmABWX3ixp9eSJUuM+6Ly0IVXWoj5UqXoS0oxCmRFQEz7jBXvxtzr+B8LFizoBeclgAlg9YUXsc8++5h9kVyAhV1YEW5MKcXsIFNKsXqIcX7tu+++vQAvAUwAqye8Ntxww+C6664zZfNRACsaYr3qxtQU2O81Y1kLPMIujMxF1WnDOHClgJcAJoDVD17E0KFDjftiV2ULsDwh1o0bqwJiXM9j8hzt6+Z3Hpfby3JjRaQU1YYqHzdmg/txvpDBqLHzEsAEsHrCi7kv2kXhvsIAi4KYL24s7wIPnsvTTz8drFq1Kjj//PODqVOnmgWrhx9+eHDYYYcFo0ePDiZNmhScffbZpkP5k08+aZ6XmgL7mVIso8DDAoy/X7ZsWd3hFey+++4CmABWH3gRfDCef/75AQALQ6xXU4rE2rVrgxUrVpjOI8OHDzcDAlVldOBna3m2keGnDbqRkybiAz9mzJhg8eLFZmErqUafCzy0Zix/N2av5yfn9uDBg+tSbRgFLwFMAKsXvIhZs2YFr7/+uoFXO4AlgVgUyHxMKXKZ17h8+fJg3Lhx5oMNmL73ve8F3/3ud4PvfOc75qcNrrcB0FyosfRg7733DubPn2+OX9iRqbN970LMvY3nO23atLo6LwFMAKsfvDbbbDPTdcO6rywQ89mNRaUUuf7xxx83gw0fWiBkobXNNtuYnzaiIGYBFuXMDjrooODuu+8282RKKfb+PmPh69esWZOr6yoZXgJY0wFWJ3gRe+yxR/DKK6/EAqwbN+ZjgQd/w+aEzGmRJrTQ2nrrrc1PG+0AFgcxUo4EAxLbbvB/61bgoZRiOjcWvo7zlB6JNUsbCmACWP3gRZxxxhkmfQjA8oCYz2vGuB8FGqzZATgWXDaiAJbWhVmIkVakJRfPuWmd7Zu2Zsy9zHOh0KeGzksAa6paEOqrI7w23njj4M477xwAsDpALIsb4/ZbbrnFFGkAHYC11VZbmUgDsU4uzIUYKUXSlPxvVSn2dkrRBvejorem8BLABLB6wIsghfZXf/VXBmAWYi684iBWtwIP4qGHHjLzU4AHaH3729/uB1gcxDq5sKiCDhdgFmLTp0836UutGevtlKIF2IsvvmhgU6O0YX/sscceApgA5j+82LDyiCOOCF599dXgtddeKwxiPrgxXuMxxxxjgGLhlQRgebkwYs6cOWatmZoC93ZTYG7jPWVRc82cl5kPF8AEMO/hZQF27rnnmjVQFmBxacS0EPNpzRi3XX755WbgAFTf+ta3+gFWpgtjTuzqq6/ub0ulNlT1b0MVBzGez/HHH19HeAlgAlg94LXRRhsFN910UyzA8gCZDwUezzzzTDBixAgDI+BlowoXxgB211139eQ+Y0kLPHo9pchlHsN+aapJ2lAAE8DqAy/iG9/4RvDwww8PAFjeEIsCWJlujNtZXMxcH7CKAljeLiyqrN6F2J577mn2W9M+Y71Z4MHv3I/iKMBUI+clgAlg9YAXYTeuDAMsDciyQixJcUceEGN928iRIw2MvvnNb5roBLGiyuotwAh6K3Ic8tiiRSlFv9yYBRit2YBKzeAlgAlg/sOL2G+//cwAbwGWFWI+g+zHP/6xGQyAVVKAJYFYty6MmDlzpmlp5cM+Y0WkFJu8zxi3Mf86bNiwuqQN+2Pw4MECmADmN7wIqvKozgNgvQgxfr/wwgsNWCy8qnZhLsRIa9KHMWqvMRV41Cel6ILMvY5jwu4FNYOXACaA+Q8v4oILLjAAo4u6C7C8QZbXerE0a8YAGI87ceJEA6Qtt9zShG8ujIGG7VuK3vW5CIipDdX6EHMv8xinnnpq3eAlgAlg/sOLCsTVq1e3BZivbiypE+O1HXjggQZSYYClhVhRLox5SNbi8XyLhli3IFNKsTPE3OB+119/fd3gJYAJYH7Di2Cgfuyxx8wgnxZiPhR4JIHYs88+az6cwMoCLA5iVbowIHbxxReb7vVR6USlFOvnxizAOAeBTY3gJYA1HWC+w4ugS8AvfvGL9QAWng+rsxtjuxQGBxdeZbmwpIubLcAY3B555BEDsG4gpjZUfkGMY08WIA5cHsJLAGsywOoArw022MD05nv55ZdjAdYLEMNhUgHGejeiKBfW7eJmCzHK/XneVUKsTm4sj5RiEW2o3Ms8PvNgNYIX6xQFsKapBaO+OsCL2HTTTYMbb7zRlNAT7SCWpLjDhwKPKIg9+eSTqQBWVYspCzBi4cKF/anEIkBWFsSaXODhXua+fNbqAi8BTADzGl64LwbOJ554YgC8LMBs9IIbe+GFF8yH2AIsLcSqcGEMSOwO4ALMNzemNWOd3ZgLMDrTA4aawEsAE8D8hRdB1RvpwygH1ksQ4/kx/5AVYGW7MAuxKVOmmK1XwhDLul5Ma8aqWTNmg2MwZswY39OGbghgApif8Npwww1Nk1EXYBZiYYAlTSn6ALKoNWNcf9xxxxlY1cmF7bDDDqaPXjiVWJYbKyul2IQCD+vCrrrqqlhweeS8BDABzF94uenDKIDFObE6u7FFixYZIH3961/3woUlLas/7LDDTEFHVRBTgUd+EOM2zlfaStUAXgKYAOYnvIhTTjnFlM8DMAsxF169BrFHH33UAMIFWDcQK6PRry3ouO6668wGmEkhFgUwpRT9SCny+PS+9DhtKIAJYH7Di4H47rvvDl566aUBAItyYXVPKVqQ8ZjM+QGwJBDzpcUUABs+fLh5/QDMRtEgi4KYj/uM1SmlyGXuy7IOwOGx8zIxZMgQAUwA8wtexKRJk0xFVBzAfHZjPB5l8bfffnuwePFi08dx1qxZZo0NrjIcrHOzcdBBByUGmC+Nfq0Lmzx5cjBnzpz+OP/88wcEx8HG3Llz+2PevHkDgqbGblx00UX9QRcQG5dccsmAuPTSS/vjsssuGxDss+bGggULTLAUwA3mXMNxxRVXDAhSvcSVV14ZGcwhucE5sGTJErPDNQ2ROS9YCM65CdQBXxhqVbgx9zLQZpdmz+ElgAlg/sGLAfj+++836UMXYGVDLApkURAjeC633XabgRAfLOCx+eabB5tttll/cNnGFlts0R9f+9rXBoQFWN1cGEHXehsUeNjYcccd+2OnnXYyEbfzb1QJd9zAGTc4xg2A/zvomdhrr736Y+jQof3B/I+Nvffeuz/oCGMDx2lj3333HRBs/ePG/vvvPyAOOOAAE3xZGTduXHDmmWcGa9asMY4dZ1elG7MOjL+lcTPHycO0oQAmgPkJLyoP+UCzLioKYFEQSwqyvFOK/OR58q2bgQzIsPDaDRdgSSGWBmA+NfptB7EogMVBLG4NUtQAWibAskIsDmA2WD4xYsQIU76Os+ScsiCrEmL8H9ysp85LABPA/IIXwQeE/DvpQwAWB7GqU4r83cqVK80ABWg22WSTAREHsTiA5QUxH1yYC7GmuLAwxJK6MBdiLshIZzIvWjXEKMzh+XgKLwGsyQDzDV4M8DfccIOBVxhgNrK4sTwLPAi6T5x++ulm4AdWG2+8sYkkAKubC8vSYioPF+ZCrBdcWCeIWYDZOOSQQ4KpU6eaylTmyKootbcQY76O4+ZR2lAAa7JaUOrzDV6kDiloeP7552PhFQexMt0Y69LGjx9vYGLBFQWwOrqwvBY3p3VhLsQ6uTAXYr3kwsIAw4kxRzZhwgQzt0pRRZHFHWFoudfxPyhe8cx5CWACmB/wIvjws6UI+X8XYL5AjPj5z39uytyBC8Bio812EGs3H1a1Cyu60W+vubA8CjqSphHDEPvhD38Y3HrrrabMv6yUYvh6ljHQNswzePGeCWACWLXwYjC86667jPuKAlgakBVV4MFmf0ceeaSBCeByo1sXVjTE5MLqNxcWhhiun88I6cSytmdxr+O+rFfkC5wHaUP3PRPABLDq4MWgyhYOzCu5AOsGYnm7Mf6edVxAAWCR7owDmFxYurL6PF1YHMR8cmFZUonEwQcfHBx77LHmi5S7ZqzI9WLh4L58HtgLzgPnJYAJYNXCi0GWFkQuvFyAxYEsK8SyFnisWLHCDMbACXiFASYXlsyFpUklpnFhVRV0uAArOpVI0HPyrLPOMt3/y+qlGBV8jgCqB/ASwASwauDFoOrCy0YUwKpIKVqQPfPMM2bgADAWXlEQa5oLq9vi5rQAcyHmiwsjlTh27FgzH0aLqrJ2fI5yYnzWDj300KrShgKYAFYdvBgUKZd/7rnnIgGWN8S6SSnSCojB3aYO4wCW1YVVBbEyG/1qcXN+Lozyeqp17RqxshsCuxDjCx6dRCpyXgKYAFY+vBiMKAu28LIRBlgakBWVUuQ5ku8HMm65fxYXlqWsXi2myl/cHOfCymwxlcSF9fX1refCyk4pcr8PPvjAtE/jeFYALwGs6QArC14M8KNHjw4efPBBMxENHPKEWN5ujKA/HYMwzz0JwNKkEnvZhTWtxVQVc2GzZ8823fzT7jOWd0qR+9DZn5ZTHLuS0obu+yOANRVgZcGLwfecc84JnnrqqX5wufByU4hxECvbjXGZQQIoAKrwomu5MDX6rbIikc7/nNvudi1lb8/iBv+LL3y85pKcl31PBLAmAqwseDGAXXvttaazdZzzSgOyrG4sLcT43zhGN33Yiy5MjX7rNxdmu9j/+Mc/Xm+/sSpTijwmbaeYpysJXgJYE9WCUl8Z8OIE40MGvKjmcwFWBcTSpBR/9rOfmQ8eAIprfZV3QYca/WYvq+/VRr9xLmzUqFFmjzG61ifdMLPolKK9D8UddA8pMG0ogAlgxW6JQprjoYceGgCvMMDSgKzslCLgZWAFRnGvtekurKoWU2lcmAsxX1tMhSGWxIUxD8YmoexAnWbX57zdWNx1dO2gj2OBzksAE8CKcV58wFx42egWYkW7MRdgq1atMgM0cEoCsLosbi6j0W9VLqxJjX5Zg0V3GHZ0BmBlQ8yFVhzE3nnnHePECoSXACaA5eu8+IBRaUjBhgswCy838oJYku4dad0Y69QYtHld7V53Hi5MjX7r7cKqmAtjnmnGjBnBxx9/3A+wIkGWJKUYdZnWU6Q7c04buu+DACaA5eO8+MD+5Cc/CZ588sn1ABYHsrLdWFKILV++PDXA1GJKjX7TlNV30+i3HcCqTClGzYvx+cU15uy87LEXwASw7uHF4HL33Xf3wysOYD5ALAnIVq9evV4KUS6seBemRr/JUokAzKYQ6VCfFGJRICuyDRXX8zf33HOPOS45w0sAE8C6hxcD4k033TQAXhZgSSFWdEoxrRtj6woG0bQAkwtTo98yFjdTxDF37tzgj3/8owFYHMSqTina4PEvv/xyc/xyhJcAJoB1By8G4XPPPdfsVOzCKwyxOrkxAPbYY4+ZDxjQSXI8qmr060KsFxv9pnFheZTVp230W9XiZlqcLVu2zGxyaQHWDmRVFHiEoQZsjzvuuDzhJYA1HWDd9jZkgpY1U2H3FYZXVW4sK8R4fBaLApJuAdbLjX7lwsp3YcwncW7ee++9Zh1YGGBFubFuU4pcx2eU15QTvASwJgMsj52UWS+F+wJgURCLAllWiEWBrKiUIpdZZ8Pgn/TY+NxiStut5FvQUeVcGJ042NySBfdxAPM1peimEnOAlwDWRLUG375u4cWgO2/evODnP/+5AVhaiNUhpXjHHXeYQTVqHkwuTNutVFVWzxowvlzRzBeA2UgDsqpSitzno48+MinQHODF8RXABLB08CL4kP30pz+NBFgZbqyMAg/+DwsxbRoxyrVW6cKa2mKqyY1+mf9iOxWq+v785z8PAJjvbswGf0eVL8esS3gJYAJYengxQN54443B448/3hZg3bgxH1KKBLtGM0DzursFmBr9qtFvty6MFm0zZ84M1q1bFwmwdiArE2LtQMZtv//974MjjzwyFlyd4CWACWCZ4MWgzDdACjcswIqCmA8pRZ6DW8yRN8TkwprtwtIsbrbuiyUeVB8CsLQQ82XNGLexsS3HKaPzEsAEsHTwIhgw2R7FhZcLsG5B5ltKkd/5oDFoAqMkAFOjX7mwIlwYi5cvuOAC033DwsuNbiFWRkrRvZ45PL4cdgEvAUwASw4vBmNOmocffrgjwHrJjXE7BSsM7OHWUmr0q0a/ZbSYonR+6tSpZm6WHZBdB9YJYj6lFN3LPMbSpUuzpA0FMAEsPbwYbC+++GKzyBeA2SjCifkGMf73tGnTDBSylNWrxdS2lSxuTuPCfG0xxdzXUUcdZYqmLLzc8NGNxUEsHDTOBtAZnJcAJoAlhxfBgHL//fe3BVjebsynNWO8DjoJAIledWFq9OvXXBjwotjhvvvuM50sogBWRzdmg+dz4oknZoWXANZkgKWBF3H88cebLcMBmAuxMMB6OaXIcz7ppJMMAKLSib60mJILK7/RrwuxPFwY8DrmmGPM9kRsXAm8bPQKxLidiuasAGsdQwGsiQBLCy8GTnqvPfroo+sBLA5iSUGWJ8TKaEPF/7jkkkvMgBte5CwXphZTecyF0W3jtNNOM+cq8PrTn/40AGB5gMyHlCK3Ma/Ha0/pvOzxE8CaptbA2pcGXgSDC4snKZ8nwgDrFTeWNKVI0EZr/Pjx67kxNfrt/RZTRTX6ZT5owoQJZneHDz74oB9eNvKGWFVuzP7OT1KjfI4ywEsAE8A6w4sYPXq0mUR2AdarEEvjxvj7VatWmY4dDPAcK7WYkgtLCzDANXHiRLNw/o033gj+8Ic/GHiFAVYUyMp2Y+5lHo/lASnShgKYAJYcXsTs2bPN/BcpxKwQ6wZkPqQUo0BmqxS5P7tRk1ocM2aMGWiBSdSxLLMisdcb/Za93UocxNK6MNKE9DScMmWKaW7Ll8P333/fuBELLjd6DWJucJ9bb721o+uKSr8KYAJYR3gxQDLRGgcwubGBC58J/ifr5VgEzcLvK664Ipg/f35w6aWXDojLLrtsQHCfcCxYsGBALFy4cL1gEHTDdtL3sdEvALn66quN2yCuv/769eKGG27oj+XLl68XnI8EqbZwkILzdXHzokWLzGeFc+Xdd98Nfvvb3xq3BbjciIJYFMjqnlIk+Bs+L1RdpoSXACaAtYcXwaDD3kOdAFaEG6tbgUeaXZ/dYD2MG2yVEY5f/vKX68XatWsHxGuvvWaCYwJgfGwxBRx4ju+9996AoL9fOHAl4WBuKBwffvihCX4/66yzvG0xRRso+gACrXBkhVgd3Zh7HfcF5jjSlPASwASw9vByu29YgJUNsV7obF82xHi+AMPHFlNAhOcdBlheEGOHcF9bTLFFDwBrOsTC1/HaWaydEl7MHwpgTQZYJ3gxR8NCSnL0YQfmRjcg6+WUYlqQhSEWBbIkEOP/MZfkY6NfwMJr5Vs3kRVkcRAjfepro9/bb7+9H2BlgayIlGK3+4yFg+fEguaU8BLAmgywJPAipk+fPgBgRUCslws8qnBj/C+KIHxs9EvBBa/bAqwbiEWBbO7cuV03+nUhlifAmBMNA6zJbsymEHmss88+Oy28BLCmAiwpvAgqEC3AskAsCmRKKaaHWBo3ZgHmY6PfKIDlCTFKsn1tMbVmzRrThT0rxHqxSpHruT8VvCnhJYA1US1A9SWFF2XddGJ3AeZCTCnF6lOKUSDjf1Ka7mOLKQuwX/3qVybyBhkpRJ9aTLkAw4EBsDiIVeXGqkop2uu4L5W67cAV1T9SABPA2q5RYi0SHehtEUcUwHxLKarA45fmeQILH7dbASK8BguwvCF2zjnneNvot6+vrx9gvkGsCjfmAuyqq65KCy8BTACLh5cFGOuVABguLCqVKDeWvxvrNqXI6wcuPjb6BRI8RxdgeYGM388880zvGv1agN15553B7373uwEQKwtkZa0ZS7vrcxKAxcBLABPA4uFlAUZu2gWYIOb/mjHeI0DkY6NfBnpK/d95553cIcbPGTNmeNtiin6iAKwqiHUDsqIgxvXcnwX4KeFFRxMBrOkAi4OXBdiFF14YC7CyIKaUYjqI0WgYUPnY6JeBh35/ACxviBFsPFp2o9+kAKPdmAVYFMS6KfCoY0rRXuYxmGtPCS8BrOkAawcvW8RBeSsAi0ojlg0yubFkKUVaMPna6PeQQw4J3nrrrX6A5QkyLk+aNClRn8QqXBifExdgdXBjRaUU3eD/zJw5My28BLAmA6wTvCzA+Eb70EMPeQOxKJAVDbG6taG66KKLvGn0G17cTOf+t99+ez2A5QExbh87dmzljX6jAIYD41yl/2HdIFakGyN4Hux2nhJeAlhTAZYUXgTd1dkV1gKsSIj55MaSQCwKZD6kFOlq4Ot2K3Rgx4EBsSiQdQMx7ovDS9qtvkwXxkJmzg8AZiMryHxfM5a2KfBvfvMbsydYSngJYE1UC059SeFFcFI98MADkQBLAzGlFMtJKfI7g3gcwKouq6fZroVXHMSyggwwcr76sOllGGI8L94fF2C94sa6SSlymff7sMMOSwsvAUwA26jj/lTs20QX7ag0og8QU2f7gcFjsmdWVZtedmoxxRYxrgPLE2KkTwFKp00vq3BhbBfy5ptvrgewpqYU3eCzeMABB6SFlwAmgLWHF8GAx75NpBHbQaysebFecGNFphR5nwBQO4B1A7Fu58LYxwuA2cgCsjDELMh4T4BS3M7NVbqwgw46yDxP0mVEniCra0qR4LYVK1YYIKWElwAmgLWHlw0WhzIwCmL+pxSXLl2aeufmslwYRRz33XffAIDl6cY4NwFWHMCqdGGjR482ra4swPKGWF3dGH9Pv9UM8MLVCmACWHt4sRZs1KhRZg2LBVgniPlWal/FmrEqCjwIqkaTAKyKuTC2eOGYhgGWF8TYmh542egEsTJbTE2cODH49a9/PQBgTXRjYffF6x83blwWeAlgAlh7eFmA8aFncWwUwKp2Y2UsfK6LG+M+DJibbrqply6MQZ5CBuaCiLxBRjui7bffPhJiacrqi3BhJ598sgHYxx9/XDjEokCWFGJltqHiJ59BYJQBXgJY0wGWBF4Eg9qSJUtMNWJREIsCWR0hVmWBB+8P0GkHsKIh1s6FjRw5Mnj99df7AZY3xGjkGwewPFxYNxDjuVmAVQUx31KKXL9w4cKs8BLAmgywpPCywfqduDRi3dxYFRAro8CDxsvAy4ZPLow45ZRTTBspF2BxIAtDrNOaMS6zGBaAleHC0m56yUBt4eVGk1KKYaB99NFHZsf3jPASwJqoFpz60sKL4ENNOb1bzFFniFWx43ORbozLhx56aCKAVeXCaNgKwJJCLI0b474HH3xwW4BV6cKovnQdWJkQK8ONZYEYGQObPswALwFMAEsGL4JBj870No2YBGR5pxR9X/hcNMSiQGYBxrEFIi7Asriwohr9kkJkHtUCLG+IMbcGSCzAkkCsqO1WoiB29913G4DZSAKyXkophqHGY9JntRO42sBLABPAksGLYDA88MADTRm0CzClFP3obM+uAe57VbULC7eYAii8bhdgcSDLAjHOFUD1gx/8IBJiVZbVU8TBOeUCLG83lmdn+yJSiuEg5Y1j7gJeApgAlhxeBIMV64xwYXFOrAg31qSUYhaI8Xf02osCWLcuLK9GvyNGjAjWrl1rijiSQCwtyFauXGngFQewKhc300aKTTzDAOvllGI7kPHYF1xwQbfwCvbff38BTABLBi8b9Cy799572wIsDcSiQKZeiunmxdasWWOAFPe+uQCrqqBj+vTpZiNLAFYExNhPygIsqwsrai6MuUnm6ShaIPIEWd0gxnWc8/Tr7BJeApgAlg5eBIPYokWLTEViFojJjeWfUjz66KMj38u0LqzIxc3XXnvtAIDlCTJ+nzx5slko7aMLY48yunBYgOUNsboUePA7z2PWrFl5wEsAazrA0sLLBnsbMSFvU4nh9WFZ3Vgd14xVXWrPFwngEfWe+uLCvve975n3DIAVATEek0o2F2A+zYXRgf/DDz8cALCmQYzg+vvvv9807s0BXgJYkwGWFV4EA93pp59uTkbrxFw31g3EtONzcjdGnHTSSW3TwT64MOBClaAFWBTEkoIsCmK8J0AIgMVBrEoXRocQABYFsTJAVlWBRzhI9U6YMCEveAlgTVRrUOvrBl52AKQx6/LlyyMBVqYba3JKkeNMeq7dnKYPLmzGjBmmgMMFWDcQC4Ns9erV/fDy0YX96Ec/6gdYVRAr242FocZjUCmbI7wEMAEsG7xsUF1FKjFqPqzqlGIUyKrqbF9ESpGfU6dOjexp6ZsLW7VqlQFYURCjTdN2223XEWJ5urCkjX5ZA8Y54AIsDcjqCLEw0PjJeEAlao7wEsAEsOzwsgMg29fHubC0KUWtGUvuxuiKAkTi+lq2e2/LdGEAgtdkARYFsaQgi4IY17NVCQBzIdbJhZXRYopgoOU5UsSRFWJVgayblKIbfMmISx12AS8BTADLDi8bbFJ4/fXXD4BYlW6sCWvGuJ3GuO22xcniwoqAGL3uWAPlAqwbNxYGGG4UmIQBltWF5V1Wf9RRRwXr1q0zALNRJ4hlcWPuZf5P3JqvLuElgAlg3cHLDnpuKrGdG/MtpVjXAo+rr77avHcbbrhh233dfHBhixcvNgDLE2IuyG677bZ+eLWDWLdzYVldGJs1vv/++wMAlrcb8wli4etooRVVdZgDvHhcAazJAMsDXnbQYyNF2kx1glivpRTLdmO8BgZc4BUGmG8uDIBwfCzA4kDWTUqR+a9tt93WRFIXVubi5uuuuy4SYL2cUiS4jcXb48ePLwpeAliTAZYXvGzYVGK4tD4NxLRmrD3E+Hnsscf2wysKYkW6sLSNfhm8KJ8PAywvN8Z96AwTBlgaF5Z1LixJWT1FHJy/AMxGniDzOaXIdYwHAKkgeAlgTQVY3vCygx6T6ffcc08kxHrNjVWRUiR1yPvTDmBZXVgRqcRrrrkmePXVV/sjCcjSuDHeB8BjAeabC7M9EF2ANcGN8TuFG+PGjSsSXgJYE9UauPryhpcd8JjzYNFmHMCqdmN1TilyTL///e8HG2ywgQkfXFi7Rr8AgOfvAixviC1btswcEyIvF5bn4mYKWN599931ANYrEIsDGdevWLFigPsqAF4CmACWH7xsHH744caFhefCfE4pJl0zljfEkq4Z4/H4wFp4dQKYDy6M5RWkD8MASwqxTilFLpNODQMsDmJVLG6eM2dO8N5775kqxF6HmPuT13HMMccUDS8BTADLF14Ec2HsPmtdWB4ga/KaMX4/4YQTBsCrWxeWZXFzGhfG4mW6TwAwG1lBFufGKJ8HEhZgPrqwm2++2cDLjSQgyxtiZYHMht1puWB4CWACWL7wsgPdzJkzTUUiEHNBppRiupQiwTYhAOmrX/1qR4i1c2Fltpii4wK7RLsAy9uN0d3DhZdvLowOHHwBsQ4sLcSSgqwbJ1YExPj7uXPndgRXDvASwASwfOFlg5PTFnMkgVi3bqwX9xnjdzYP5X0AXjaqdGFJIAbArrzyyuDll182EYZYUpC1gxi/s2yDLvdEFhdWdKNfUukUMgCwKIhV5caKgpi9jtJ55v5KgBc7xAtgTQZYEfBiYNtmm23MJC4uLMqJNTWlGAWyOIiRhgUILrzq4MIYzHn+FmBxIOsmpchiblJ07QBWtQtjzysKOCzA4kBWd4iFg/P3oIMOKgNeAliTAVYUvGywwJSdm+MAppRiPMhWrlxpKjq/8pWvmOgEsSIKOrIubuZ9f+mll9YDWJ4QA+4WXkkhVmaLKQDGc4wCWNEpxSrWi9ngtltuucUAqAR4CWBNVGtw6isaXgSLWEkjugBLCrGyUoq+rRkjGPhceEUBrBPEsrqwbltMAQeOHwCz0QliaUHGT3Y4ZjsfIgpi3bqwNIubowDGJpZ8EQFgSSHWC26M+11yySVlwUsAE8CKgRcxbNgw0x+xXRpRKcX/Axm/s1AZF+PCq04u7NRTTzXFGy7A8nZjHCdg0g5gVbswFvQz/2UBlgZkWd1YlRCzv/NYFHCVBC8BTAArBl4EH/w1a9b0pxE7ObE0bqzXmgITbPYHQL785S+b6AQw31wY0OA9AGBFQmzBggVmqYYFWFYXVlSLKQB23nnnmWKGMMB8SSkWATKC/z9x4sSy4CWACWDFwMsWcrBjcxzAmuzGXHDxWCz6BUAWXlEA892FTZ8+PXjxxRf7ARYFsW5Tivw9BQIAzIWYTy4MgN1xxx0GYDayQKxuKUWu47UdccQRZcFLABPAioEX38iZx1myZElbgBUFsboUeHBMWDPlgqsdxNK6sLJaTAEIjm8YYHm7MRZHA6kwwJJArOi5MAuxvffe27weF2A+u7G8IMZldgdg+UBJ8BLAmg6wouBF0A+PdA+FHJ0g1m1KsW5rxvidRrcMsF/60pf6oxPAqnJhnSB25plnBi+88IIBWJ4Qc0HG73QkwdlbgMVBrFsX1k2j36lTpwZvv/128M4776wHsaxuzPeUog2WOBx66KFlwYsvfwJYUwFWJLwIFrRedtll/QBzIVamG/Mtpcj/ZpADJi68ogBWBMTydmEM4rwuCy83OkEsTUqR/wFQkgAs6+LmPFpMsf8X8LKRF8R8Tylyvd3epgx4CWANBljR8LIObOHChWZH1rAL64WUYtp9xvgd18Xg98UvfnE9eKWBmC8ujPd9/vz5xn25DqwdxLK6MbalB142fHRhlM/TPsoFWFKI+ezG2kHMvY7Ky1GjRpUFLwGsiWoNPn1Fw4ugoStzYADMOrCkEPMxpdiNG2M5wdixYw1sgJeNrAAr0oWlaTFFqogB2wKsG4i1c2Ns5klvwa233joSYr64sKOOOsrMA9kUYhaQZXVjVaUU3eB5TZgwoei0oRsCmACWP7wIBhh2ZHUdWBTAejmlyOs6+eSTzXFywdUOYFW4sCwtpnDYdFunK7wLsCLcGHvMAa84gPngwgieJ/Byo2o3VibEeGx6VJbgvAQwAaw4eBF8+FevXt0PMBdicSDr1o35kFLkJ69vxowZxoV+4QtfMBEFsDq7MObx6EkIwGwUATEeF6dnAZYEYt26sCyNfnfffXezLCIMsKLdmE8FHtyXPdBKgpcAJoAVAy+6STAf0NfXFwuwPNyYTylFLt92221mk0VevwVXFoDl5cKKavTLgM3rdeEVB7EokKVJKVIUAbS22mqrTC4szxZT7VwY6cM33njDzAMRSUDWCwUe7u/cj3negtOGApgAViy8CNaDMPfTDmA+pBS7dWP8LwoZ+KACiTC4skKsDBeWpaCD959tXnBfYQeWBmJJQMbfsXAZeLkA89GFcUwsvPKGmM9tqMLB5xToFOy8BLCmA6xIeBHkwu+6664BAIuDWFEpxSLcGNDiJ22yjj/+eDOYAqfPf/7zJvICWFYXVlSLKQswWgU9++yz/QBLA7K0EGMO1bqvdhCr2oWRbWALmTDAkkKsFwo8bPC+jRw5sgx4CWBNBVjR8LJrwCzAwhDz1Y11KvDgd9KEbNjHoG6h5UY7gNXdheE0OA5UBYYB1o0bi4IYj8cgxzxiJ4BV7cKmTJli0odUIBJZQZbVjVWVUowCGs+HBecFpg0FsCarBam+IuFFMEiw5Xs7gOXhxspMKfIYZ599tqm+iwJXUojFAawbiJXRYor39YYbbjDwciMJyLKkFEnJASwAlgRiebqwTmX1YYBxrlt4VQWxqgo8whDjfqTUC3ZeJg466CABTADLF14E/eDoW+cCrCiIFZ1S5Heas9JhAHh0glfeqURfXBjbZLDmKwywbtxYHMT4P0OHDjX9NJMCLKsL67asft999zXP2XVg3UKsbgUe4bjzzjvLgJcAJoDlDy+CDQcp4ABgSSDmc4EHO8wyxwF4WoevUID5WlbPgEEPR+a+bOQFsSiQXXzxxQZeLsDydmF5LW5mB+rXX3/dACwKYnV3Y1kgxntKT8SC0oYWXgKYAJY/vEixzZ49OxZgdUkpchl4sb7HwisNwIou6CjLhTGAc7xdeMVBLI+UImvp6GphARYHMR9cGM17OU8svNxoakqR63mORx99dJHOSwATwPKHF8UbDC6LFi0aALAyIZZXSpE0yPDhww1ULLzSQqxuBR1hF8b7yZ5upPRsJAFZN26MfcX4EtQJYD64MNomsU8ZDsx1YWncWK+kFN3fue+sWbOKhpcA1mSAFQEvgkGByX4AlhViVacUuR/l4gAhDC+fXFiRLaaY95o3b54pD3cBlhRiUSDrBDHmTYENAEsCsTQuLE2LqaRl9cDdwqsbiPWCG3OBxk+qkPMCVwy8BLCmAqwoeBGkVUi9RQEsDmI+uTG7MJljEQWvprgwWkU9+eSTBmB5QiwOZNyXxe+cQ0kBlgRiRbkwijdIeYYBFgWxogs8fIMYwRq+Ap2XANZUtQbmvqLgRQwbNsy0kIqCVxaIle3GSB0OGTIkMnXYFBfGlhhsAWPhFQexPFOKV1xxhYGWC7C0ECvTheFO165da/bAIpKALKsbq1NKkeC222+/vWh4CWACWL7wIjgxKTu3DiwvkJVR4MFlmpGSPmsHr152YSyBYPnAU089tR7AuoFYO5CxOJpF0u55FAUxH1wYMWjQIFOVaeGVBmJNKPDgOj6bQKaAtKEAJoAVAy+C8tkwwHyBWKeUIv93n332MaDIE2A+urCoFlO77babOUbAy42sIEsCMa6bPHnyeueUzy6MQhOKN1wH1g5kSSDma4FHGoi5l0nHH3zwwUU5LwFMAMsfXgw2LPiNAlgWkJWdUqT7g+20kQRgveTCWNPE+8O8F5EXxDqlFK+99loDpqhzqyoX1q6sns4bnFfAy40sbqwXCzxsUMkbBbAc4cXjC2BNBlie8LIDjnVgzCXlAbGyUopcx+aTDOZJ4VU3FxYHMQZm5i0tvMqAGEFj5F133TX2/CrDhaVt9HvMMccEr7766noAywqxXizw4Ho+U24KMce0oYWXANZkgBUBL4KTkMEQgPkOMRdkPB7PHRCkAVjdXRgDMw2K2YwxDLAoiOWVUuR29k6LawadFGJlNvqldJ7iBNKHNoXYCWJNKfAI/85nls9TQc5LAGsywIqCF0EPOwZEC7A8QVZkSpGBiUKCTtWHVbiwohr9Uk3H6wZebiQBWbdujDJrQNQtwMps9Dtu3DizyaYFWBqQNSGl6Matt95aNLwEsCaqNVj0FQUvgnVgN998s1mUWgTEinJjLEplgHPbRtUxlZjUhdGuiVQv1XRhgOXtxsIAo0Sf5RbhTIBvLsyFGOnD1atXm/RhGGB5QszXpsBpIMZt7MxcUNpQABPAioEXwYedThwAzEYSkFW9ZmzJkiVmUExTwFGXVGIYYHvuuac5hsDLjSIhZkHGz0svvdScc+G5WF9cWFQq8YgjjjBd5wGYjSQgKzulmNWNdZNSDAON3+fOnVs0vAQwASxfeBEMFixK5dt9WogVmVLs5Mbo38jrKgNgVbowBg1SpmF4xUEs75Qi7os0c1wlbB4Qy9uF4b7Y88uFVxqINSGl6AaPfeKJJxYNLwFMAMsXXnZAOeusszoCzLeU4pVXXpm6hL5OLoy0If0dWaQMRGzkCbEkIKNsnnMobi2ijy5s7Nixpm3UK6+8YiIJyHo9peiCLAw1Xj/HrGB4CWBNB1je8LJx5JFHmkrEJBDzpcDj6quvNgNf1jkwn8vq6Sxy5plnBo8++ugAeLWDWBFujNsoQ4/aDaFKF9ZucTPua82aNf3wSgOxOqcUo0AWBbEw0PjJlyTgUzC8BLAmA6woeBH0EqQKCYCVCbFu3NiKFStMpVmWKkSfXRiD+VVXXWX22XrsscdMlAGxKJCx7otuH3Hb+WSFWJGLm9nXirkvqg/zgpgPbizPlKIb3M6XwRLgJYA1Ua0Boa9IeBEMCBRFWBeWFGJFFnh0ghjPleKGLOvAfHVh7CTNkgYXXm4kBVleEGOXAmASt6Fq3i6s2xZTrPvi/ANeNsIQa4IbSwMx/i8NAUqAlwAmgOUPLzuYnHLKKbEA8zGlyG3k7elM0e08WNUujNdAqo7+jsDLRjcQyyOluGDBAnOuuQDLC2JFuDAKEaz7CodSiv8HMvcn7//IkSPLgFdwyCGHCGACWL7wsoPJfvvtF5tG9DWleM4555hBtEyA5e3CGKjZz4x0HXNeLsDiQFZWSvGMM84wx3fzzTf33oWxnpF+fi+99JKJIiFW95SiDW5nCU1J8BLABLBi4EXwDXbx4sUdXVhVKcUokN14440mbZS0G71PLgznxWDBcbbwspEEYt2CrBPEuDxt2rRIgFXtwqLK6tlWB/dlARYHsiamFONAFpU+LBBeAljTAVYUvGwcf/zxplWRC7Gq3FgSiPG348ePNzsS18GFWYjxvjDg4hjC8CoTYu1Axs/jjjuuH2A+uzD2ROP5ArAkEPPBjVUJMXuZlDVgKQleAliTAVY0vAjaFd10000GYD5ArFNKkd+p2OMbeB1cGFWGdP/n2LIpJPCyEQWxIkCWFGLElClTDJQswIqGWBYXRuaANkgWXnEQy9ONZW0K7MOaMRs8Bin4EuElgDUVYGXAyw4gpBSiXFg7iKUFWZ4pRR6PdWybbLKJ1y6M9UkLFy40rgt4hQHWDmRVubFTTz3VQKgsgGVZ3PzDH/7Q7FMWBlhSiDW1wINzYNSoUWXCSwBrolof/L6y4EWw7oe5pSiA+ejGCBr7sk8VDsc3iDG4U+EJbC243PAVYvxOfzzOGRdgPrkwNvXkvKDrho0kIGtigYcLM/5m3rx5icGVE7wEMAGsWHjZQcN1YVHpRB8hxmDL4OZLKnGjjTYy7oB1Xbguuh0QURCrEmTtIEYbKVxOmQBL48JIgb3wwgsDAJYUYnUu8OgmpUhwblE6XzK8BDABrFh42WAbdgavThDzqcCD+06fPt0cnzK7c4QhRg9DPqzMJbrgCkcd3BjH1Hbi8M2FsewDyAKwKIhFgazslKIvBR7u7zz+aaedVgW8BDABrHh42UGD/DjrwjoBzCc3xmOzmLVsiFlw8SFm48cHH3zQwMuNvCFWhhvj8caMGbPePFg7gCWBWLcujMINus1beKWBWBNTija4jT3SKCSqAF4CWNMBVha8CFJH5557rnFhrhPLC2JFFXjwuDNnzjQDHWutit5uhca7DPLMG1KWDLyiAFZHN8bvF154oUnXhQFWZioxDDDmFCnceP7553ODWK+tGYuCF8sjWHZSEbwEsCYDrEx42aC7ASXK4VSib24sKp3IJoyDBg0ybZq66VgfBy4G0qlTpxqX6oLLjSiIFeHGioQYx5Jmz+56sCiAdQOxNC6Mvcl4LcDLRhhivebGuk0p8pP/TVVphfASwJqo1ge8rwp42eCEJl2TFGJ5ldp368a4zPNmMS7rxNgcMosbc6FFmnDYsGHGlfD4bDQJvNxICrKkEIsCWZkpRW47/fTTzblQpQsjeB9xutZ9haNMiEWBLCvEsq4ZiyvwCAON+11++eUGOBXCi9SlACaAlQcvW/F17LHHGqcRTidW5cTSVChyPf3eWJRL2TXrxahUTAozSvMpamFubeXKlWYzTcAVBa88IOajG+P92WeffTrOhRXtwkgNP/fccwZgSSGWZ4FHnVKKNrid8zbJvFfB8BLABLBy4WWDSXM2WKQk3BeIZSm15/mzmBiY7bvvvmZxMceTgXizzTYzP21HksMPP9y8ZqoJWcPlgsuNbiFWVoFHtxC77LLLgu23375jKrEoF8agyfOw8HKjajfma4EH1/P5oSDLA3gJYAJYOfAKA4xg88gLLrhgAMTKLO7Iw43Z7h3WmfE82L2XyiwCl8l13M6WLUArHFEQiwNZWRArw43xv0844QRT3FNGWb17/u6+++7mPcF9uQ6sLIj50IYqqRtzg3OQtYiewEsAazrAynZe4e4HpNKY/0kLMZ9Sikl3fLYRBbE0biwKYt3Oi1VVas8xYkDkXAg7saJc2M477xzcfPPNwbPPPtsPsG4g5qsb6wZiYaDxO+fYhAkTfIKXANZkgFUJL7f7AWkkyutxLlEQa1fo4VtKMSnE0rixIubFfOnewWWO/7hx40qBGO3BmL955pln1gNYVW7M15SiDW7ns+CZ8xLAmgywqtKGUQ1UCdKJs2bNMim3ODeWF8SqcGPdQqzqlGLREOM4T5482ZwHUTs2dwswzt/hw4ebL0kWXm7kBbFea0PF/W655ZZg9OjRPsJLAGuiWpDq88F5hWPbbbc1+4cxdxRX3FGXXoplphSLcGNlpxT5nedML8LBgwebcwg4dQsxzmkKhjivOE7AKwpgRUOsTm7MBo+7bNmyRD0OK4KXACaA+QEvdzdcKpxon9TJiRU5L9bLbsznlCJ/R4pv0qRJZtE75xNQCreW6gQwzmfOJQY5Fs6z/szCKw3E6lTgkRfEuI5jwZcJAOIxvAQwAay6tGHUTrg29thjj+D88883KZ92bqwJBR51TilGgSxJSpH/wZY2VCmy0JvUIucQ56p1Vy6wbJqQcwfwsZ/b1VdfbZ4X7Y6efvppE2GIVeHGfN1njOC+nPekcyvusJEEXgKYAOaH8woH35632247M7nPN+gq3ZhSitV17+B+HNMlS5aY7h2AiQ0Tgdpee+1l2kDRRX7s2LFm54BFixaZ94a/dcHlRlaIRYGs7ilFNzg2VASzXrEm8BLAmg4wX+HlBttvnHHGGYnmxuqwZkwFHtkWPlug8Tx4ffZ1c5nr7b5jgMuNoiFWpzVjFmQu0Hh8PltHHXVUVVuiZIWXANZkgPmWNoyClw3c2AEHHBBcdNFF/eX2ccUdZZfba81Y/m4sy47PboQhFgWyKiBWRUoxDmZcx3l30kknJZrr8hBeAlgT1YJUXx2clw2qyGzQqomqqPnz58f2UtSasd4s8CgCYnnPi0WBrOyUYieg8bic92eddVaqXZQ9hJcAJoDVB15uMKnPXMjZZ59tJvtJLRYBMaUU67XjcxKQVTEvVoYbawc0rudYUtjCbgrAiM8PUWN4BYcddpgAJoD5mTaMgxfrxdxgjozJfVwZLYKSFHqowKP+bajKcGNVpBTTdLZvBzNu4/myEJkvekcccUQ/tHoEXgKYAFYv5xWGlw3myGhJRbcFSq+vuuqq/hRjk1OKvbxmrB3IqkgpVlGlGBU8D87jefPmmS92rtvqMXgJYE0HWK/AKxw77LCDgRkdGK644grzLbQJBR69tmasFwo8ioBY+HeeC+c1PUVtw10XVD0KLwGsyQCrc9qwHbwI5shs4MxYK8SkNXtxpSm79z2lWJcCj15JKSbpo1hUSjEMNO7Le7BgwYLg6KOPjnVaPQwvAaypAOtV5xWGlxtUMNLhgxQjraritm1RgUf9d3zuxZSiDW7nXGBei0XHSaDVo/ASwJqoFqj6mggvN9jQ8MQTTwxuvPHGfpB1Sis2cc1YlTs+N6nAoxPE7HUcZ3oUMqAfeOCBJhoMLwFMAOvNtGE7eNkgtUhLorlz5/aX4ZcBMa0ZU0qxnRuLghqPSzutMWPG9IMrDcB6FF4CmADWLOflwsvGjjvuaNro2PmxrBDTmrF6pxR9WjPmBseTPo+ASPASwAQwB2BNh5cb+++/vym/T9pTsdfXjNW1DVUd3VgU1LgvPQrZCTkLuBoALwGsyQBrYtqwHcAovWdujI7cZc6LKaXY222oskCMx6a6EACE4SXnJYA1HmByXgPB5cauu+5qJsq7hZjWjKkNVRKQhaHGfS+55BIDF8FLAJNCaoGqT/CKhpcNNkVk3VhSiKkNlQo8sroxF2Dch4X3UfBS2lAAkzIArNfThlFBYQcQO++883KBWFMLPKJgpgKP/w+yMNC4zOatShsKYFIKgMl5DQSXG6QT3Tkx7TNWfYFHN22ofFozFobZihUrzKAseAlgUkKACV7x8LIxZMiQYOnSpZVATAUevVXgEQczzovRo0crbSiASXkArKlpw3DstNNOJviAUtKctIeiUopaM9YOYi7I+J90hZHzSh+HH364ANZEgMl5dXZeFl42Tj755FgX1vQCD60Z6wyyOKBRtKFFypngJYA1US1I9Qle6eBFsEaMhc51gpj2GfMHYlGujPdn5MiRShtmg5cAJoApbZgEXjYYbMJ7i1UBMqUUuyvwqGrNmAsxHmfmzJlyXtnhJYAJYHJeSeC18847m9hll12COXPmtN3pWSnFYtxYXVOKcTBjTjVcMi94pYKXACaACV5J4WWDzTEpeS4KYtpnzM+UYjf7jIVBxmNNmzZNacPu4CWANR1gSht2ThuGAYYLo0sHW7AkhZjWjPmdUowCWd4pRTfYh87ttiF4ZYKXANZkgMl5pXNeLsDoXL9q1SoDsDDEejGlqDVj2SAWBTP+95QpUwSv7uElgDVRLUj1CV7Z4WVj9uzZkfBSSrGYlGKd14xZiPHz+uuvN7BR2rBreAlgApjShlkBtu+++5pUUBaIVZVS1Jqxat0Y/3/SpElyXvnASwATwOS80oLLjVNOOaV/LqwMN6Y1Y/Ut8OC6hQsXynnlBy8BTAATvLLCixg8eHCwePHiARCLA1laiGnNWG+sGbPBMR8zZozglR+8BLCmA0xpw+wAY7sVgg/7TTfdlMiJac1YM9eM8ZgzZswQvPKFlwDWZIDJeWV3XhZeNk444YRgzZo1A1xYlQUeTUsp+lzgwX0WLFiQqHBD8BLApA5qQapP8MoPXsQee+wRXHzxxcaFJUknas1YM/YZI1auXGkGaMErd3gJYAKY0oZZ0obhYOPLvffeu38+LCnEtGasujVjRaUU3Z8c//HjxwtexcBLABPA5Ly6cV4WXjYYjK677rp+iHXrxnxbMxYFNO0zFh0cm2OOOUbwKg5eApgAJnjlBS8bfDBvuOEGMydWh5RiVpAlcWVNLPDgMq9x6tSpglex8BLAmg4wpQ27SxtGxW677WY+oDixtBCrOqVYVKFHU9aM8ZNjQqsowatweAlgTQaYnFe+zsvCywbdFtgAMw5iPqYUk3bziIqov7WA6wS0XkkpcrwnT54seJUDLwGsiWpBqk/wKhZeNvbZZ59g3rx5AyDmkxtLCjMbPKfly5cHl19+eXDeeecF06dPD44//ngz13P00UcHRx55ZDBhwgTzk8vHHntscPLJJwdnnHGGqdJcsmSJaYLMc3LBlia16FuBhw3aiqlgo1R4CWANBdgspQ3zTxtGxaBBg0y3jpNOOskM3HFOrMo2VO3i5ptvDhYtWhScfvrpwVFHHWUGP3pAUnE5bNiwAcF1cQHIieHDhwcHHHCAGcyA3plnnmkqN3mdYWfWDmRVphTDMOP/XHjhhWZAFbxKhRcxSyN68wA2Uc5rl1Lg5cbIkSONc7n11ltzSylmTStGAc1ez/+75pprjHNi0B06dOh6sEoDLxdg4QBoAJEBksXgHB+Og4VZHhDLK6UYBhqXeS+TFGsIXocXFRM1ojcPYLsIXuXCy8aQIUNMys0t8IhyY0XvMxYVuC3SnePGjTNgAVw2ioCXBZgbwIwB77TTTjPtuUgzuiBLWqlYhBtzg+c1Z86cRK5L8Dq8yNhFI3ozIfaZ0oblwmv33XfvDwZr5o/ccvsqIMbtgGLmzJlmUARILri6dV9p4BUGGUUwVPMtW7ZsAMjiIJbUlcW1ogoDzQWWvY7nMX/+fDPPp67ylcPrM43kzQXYWjmvauDlBgM5hQ5ss7F69erUqcWsXe5JfZGuAxDMSYWhVbbzCsPLDXa/njhxogEHr6uTI+u2HVUYZNyP9+SCCy4w7jQpuASvQuFFSn6tRvKGqgWqpYJXtfAi6KFIUOjBQIYro2iCdF5U1WKWOTKC+9xyyy2mCpCCDAa2OGj5BC839ttvPzNwkV5kV2NK9NvBLM0cmYWZe5l5QZZBTJs2zRwvHGHSjSgFr8LhRSzVSN5cgI1Q2tAPeLkByPbaay8z+OCOKFVnEKVEG9eU1JUBPZrIUuF37rnnGpfHYAl8eHwbWeBVZNqwE8Bs4BpJ482ePTtYunSpgTSFH1EFH3GOLBzAkHTulVdeGZx66qnBqFGjDIgsuNIATPAqHF7ECI3kzYbYp3Je/sDLAiwce+65pxn8GZQYtI877jhTIci8FW4KR8KAi1NgES33YSBhoAdGLrC6hVcVzisMr3CQYmTgJc3IMaEQBQixXg0nC9DtnB+gA/5cf+211waXXXaZgSDLHFjDxTGOgpbg5R28PtUILoD1CV5+w8sCLC6oaoyLOHD1ErwswKIClwZ0wrCw+3Jxu71Pp1Da0Ct4EVrALIB9f6jShn7DKyvA8oaXD2nDNPAKg6xd5AUwwas0eBFDNYJLn2vBap2cV/3gJecleDUYXus0cksWYNMEL6UNBS+lDWsCL2KaRm7JhdgnShsqbai0oZxXDeD1iUZsaT0XJueltKGcl+DlObzkvqRotWC1TvBS2lBpQ6UNPYaX5r6kWIANVdpQaUOlDQUvT+GlykOpI8SWynkpbSh4CV4ewktto6SOANuoBapPBC+lDZU2FLw8gtcnrdhII7TUUS1Y7dKKz5Q2VNpQzkvw8gBen7VCe35JqSA2Uc5LaUPBS/CqGF6EdlyWMkHsfMFLaUOlDQWvCuF1vkZiKbNasOpT2lBpQzkvwasCeKlZr5QvxOS8lDYUvAQvwUuqG8TOF7yUNlTaUPBS2lCqpVqwmtiKz5Q2VNpQzkvwKqjaUAUbUqEQ26UVn8h5KW0oeAleOa/zUqm8VArENmqBaqngpbSh0oaCVx4dNrRIWSpdLWANbcU6pQ2VNpTzEryyNOZVb0PJB5BNa4HqEzkvpQ0FL8EryX5e2hJF8k4tYAGydUobKm2otKHgFeO4BC7Je5ANbYGqrxWfKm2otKGcV6Ph9SlrupQqlGqpFrBGtGJpK9a2IPWZ0oZyXoJXT8OLUvi1/1uYMUIjoNRTagFrl1ZMbMWsVvS1IBUbLVBFRgtUkdECVWS0QBUZLUjFRgtUkdGCVGS0IBUbLUjFRgtSsdECVWS0IBUbLUC1jRakYqMFqdhoAapttADVNlpw6hgtQLWNFpzaRgtMiaIFpkTRglLiaAEpVbRAlClaAOoqWoApIma1YmIrVAYvSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSZIkSVLD9P8AG/ZQNWGiN0AAAAAASUVORK5CYII=", 0);
            ownNote = BitmapFactory.decodeByteArray(decode, 0, decode.length);
        } catch (Throwable th) {
            Diary.note("badge: " + String.valueOf(th));
        }
        return ownNote;
    }

    public static Badge byId(String str) {
        for (Badge badge : numbered) {
            if (badge != null && badge.id.equals(str)) {
                return badge;
            }
        }
        return null;
    }

    public static Badge[] of(String str) {
        if (str == null) {
            return null;
        }
        return known.get(str);
    }

    public static String marksFor(String str) {
        Badge[] of;
        if (!isEnabled() || (of = of(str)) == null || of.length == 0) {
            return "";
        }
        Badge[] badgeArr = numbered;
        boolean margytEnabled = margytEnabled();
        StringBuilder sb = new StringBuilder(of.length);
        for (Badge badge : of) {
            if (!badge.id.startsWith("margyt.") || margytEnabled) {
                int i = 0;
                while (true) {
                    if (i >= badgeArr.length) {
                        break;
                    }
                    if (badgeArr[i] == badge) {
                        sb.append((char) (FIRST + i));
                        break;
                    }
                    i++;
                }
            }
        }
        return sb.toString();
    }

    public static boolean isMark(char c) {
        char c2 = FIRST;
        return c >= c2 && c < c2 + 256;
    }

    public static Badge byMark(char c) {
        Badge[] badgeArr = numbered;
        int i = c - FIRST;
        if (i < 0 || i >= badgeArr.length) {
            return null;
        }
        Badge badge = badgeArr[i];
        if (!badge.id.startsWith("margyt.") || margytEnabled()) {
            return badge;
        }
        return null;
    }

    static String uidForMark(char c) {
        Badge byMark = byMark(c);
        if (byMark == null) {
            return null;
        }
        for (Map.Entry<String, Badge[]> entry : known.entrySet()) {
            for (Badge badge : entry.getValue()) {
                if (badge == byMark) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    public static synchronized void start(Context context) {
        synchronized (Badges.class) {
            if (started) {
                return;
            }
            started = true;
            TtyBadges.start(context);
            byte[] read = Net.read(file(context));
            if (read != null) {
                apply(read);
                prefetch(context);
            }
            byte[] read2 = Net.read(file(context, true));
            if (read2 != null) {
                apply(read2, true);
                prefetch(context);
            }
            final Handler handler = new Handler(Looper.getMainLooper());
            final Context applicationContext = context.getApplicationContext();
            handler.post(new Runnable() { // from class: cat.ttcuz.framework.Badges.1
                @Override // java.lang.Runnable
                public void run() {
                    if (Screen.now() != null) {
                        Badges.refresh(applicationContext, false);
                        Badges.refresh(applicationContext, true);
                    }
                    handler.postDelayed(this, Badges.EVERY);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void refresh(final Context context, final boolean z) {
        Net.away("badges", new Runnable() { // from class: cat.ttcuz.framework.Badges.2
            @Override // java.lang.Runnable
            public void run() {
                boolean z2 = z;
                // our own server's request also says, once in ten minutes, that this phone is here (ModStats)
                Net.Answer fetch = z2 ? Net.fetch("https://margy.narez.xyz/api/badges", Badges.margytTag)
                        : Net.fetch(Badges.SOURCE, Badges.tag, ModStats.HEADER, ModStats.here(context));
                if (fetch == null || fetch.unchanged || fetch.body == null || !Badges.apply(fetch.body, z)) {
                    return;
                }
                if (z) {
                    Badges.margytTag = fetch.tag;
                } else {
                    Badges.tag = fetch.tag;
                }
                Net.save(Badges.file(context, z), fetch.body);
                Badges.prefetch(context);
                Diary.note("badges: " + Badges.known.size() + " accounts, " + Badges.numbered.length + " badges");
                Badges.told();
            }
        });
    }

    public static void refreshNow(Context context) {
        TtyBadges.refresh(context);
        if (context == null) {
            return;
        }
        margytTag = null;
        tag = null;
        refresh(context.getApplicationContext(), false);
        refresh(context.getApplicationContext(), true);
    }

    public static void tell(Runnable runnable) {
        waiting = runnable;
    }

    public static void stopTelling(Runnable runnable) {
        if (waiting == runnable) {
            waiting = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void told() {
        Runnable runnable = waiting;
        if (runnable == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static boolean freeStillOpen() {
        if (serverNow <= 0) {
            return false;
        }
        return serverNow + ((SystemClock.elapsedRealtime() - readAt) / 1000) < freeUntil;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void prefetch(Context context) {
        try {
            for (Badge badge : numbered) {
                if (badge != null && badge.image.length() > 0) {
                    picture(context, badge.image);
                }
            }
        } catch (Throwable unused) {
        }
    }

    private static boolean apply(byte[] bArr) {
        return apply(bArr, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized boolean apply(byte[] bArr, boolean z) {
        JSONArray optJSONArray;
        synchronized (Badges.class) {
            try {
                JSONObject jSONObject = new JSONObject(new String(bArr, "UTF-8"));
                JSONArray optJSONArray2 = jSONObject.optJSONArray("badges");
                if (optJSONArray2 == null) {
                    return false;
                }
                long optLong = jSONObject.optLong("free_until", 0L);
                long optLong2 = jSONObject.optLong("now", 0L);
                if (!z && optLong > 0 && optLong2 > 0) {
                    freeUntil = optLong;
                    serverNow = optLong2;
                    readAt = SystemClock.elapsedRealtime();
                }
                HashMap hashMap = new HashMap();
                HashMap hashMap2 = new HashMap();
                LinkedHashMap<String, Badge> distinct = new LinkedHashMap<>();
                for (int i = 0; i < optJSONArray2.length(); i++) {
                    JSONObject optJSONObject = optJSONArray2.optJSONObject(i);
                    if (optJSONObject != null) {
                        String optString = optJSONObject.optString("id", "badge" + i);
                        String optString2 = optJSONObject.optString("image", "");
                        if (z) {
                            optString = "margyt." + optString;
                        }
                        String str = optString;
                        if (z && optString2.length() > 0) {
                            optString2 = MARGYT_IMAGES + optString2;
                        }
                        Badge badge = new Badge(str, optString2, colour(optJSONObject.optString("colour", "")), localised(optJSONObject, "title"), localised(optJSONObject, "text"), localised(optJSONObject, "button"));
                        if (!isCreator(badge) && badge.id.matches("[A-Za-z0-9_.-]{1,64}") && (optJSONArray = optJSONObject.optJSONArray("users")) != null) {
                            Badge canonical = distinct.get(badge.id);
                            if (canonical == null) {
                                distinct.put(badge.id, badge);
                                canonical = badge;
                            }
                            for (int i2 = 0; i2 < optJSONArray.length(); i2++) {
                                String optString3 = optJSONArray.optString(i2, "");
                                if (Ids.uid(optString3)) {
                                    List theirs = (List) hashMap.get(optString3);
                                    if (theirs == null) {
                                        theirs = new ArrayList(2);
                                        hashMap.put(optString3, theirs);
                                    }
                                    Set ids = (Set) hashMap2.get(optString3);
                                    if (ids == null) {
                                        ids = new HashSet();
                                        hashMap2.put(optString3, ids);
                                    }
                                    if (ids.add(canonical.id)) theirs.add(canonical);
                                }
                            }
                        }
                    }
                }
                int min = Math.min(distinct.size() + 1, MOST);
                Badge[] badgeArr = new Badge[min];
                badgeArr[0] = CREATOR_PLACEHOLDER;
                int i3 = 1;
                for (Badge badge3 : distinct.values()) {
                    if (i3 == min) {
                        break;
                    }
                    badgeArr[i3] = badge3;
                    i3++;
                }
                HashMap hashMap3 = new HashMap(hashMap.size());
                for (Object pair : hashMap.entrySet()) { Map.Entry entry = (Map.Entry) pair;
                    hashMap3.put((String) entry.getKey(), (Badge[]) ((List) entry.getValue()).toArray(new Badge[0]));
                }
                Badge[] badgeArr2 = (Badge[]) hashMap3.get(CREATOR_UID);
                Badge[] badgeArr3 = new Badge[(badgeArr2 == null ? 0 : badgeArr2.length) + 1];
                badgeArr3[0] = CREATOR_PLACEHOLDER;
                if (badgeArr2 != null) {
                    System.arraycopy(badgeArr2, 0, badgeArr3, 1, badgeArr2.length);
                }
                hashMap3.put(CREATOR_UID, badgeArr3);
                if (z) {
                    int i4 = min - 1;
                    Badge[] badgeArr4 = new Badge[i4];
                    System.arraycopy(badgeArr, 1, badgeArr4, 0, i4);
                    margytOrder = badgeArr4;
                    if (badgeArr2 == null) {
                        hashMap3.remove(CREATOR_UID);
                    } else {
                        hashMap3.put(CREATOR_UID, badgeArr2);
                    }
                    margytBadges = hashMap3;
                } else {
                    ownOrder = badgeArr;
                    ownBadges = hashMap3;
                }
                mergeFeeds();
                cat.ttcuz.framework.Badge.refreshProfiles();
                return true;
            } catch (Throwable th) {
                Diary.note("badges: unreadable, keeping the last ones -- " + String.valueOf(th));
                return false;
            }
        }
    }

    private static void mergeFeeds() {
        ArrayList arrayList = new ArrayList(MOST);
        for (Badge badge : ownOrder) {
            if (arrayList.size() < MOST) {
                arrayList.add(badge);
            }
        }
        for (Badge badge2 : ttyOrder) {
            if (arrayList.size() < MOST) {
                arrayList.add(badge2);
            }
        }
        for (Badge badge3 : margytOrder) {
            if (arrayList.size() < MOST) {
                arrayList.add(badge3);
            }
        }
        HashSet hashSet = new HashSet(arrayList);
        HashMap hashMap = new HashMap();
        Iterator it = Arrays.asList(ownBadges, margytBadges, ttyBadges).iterator();
        while (it.hasNext()) {
            for (Object pair : ((Map) it.next()).entrySet()) { Map.Entry entry = (Map.Entry) pair;
                List list = (List) hashMap.get(entry.getKey());
                if (list == null) {
                    list = new ArrayList();
                    hashMap.put((String) entry.getKey(), list);
                }
                for (Badge badge4 : (Badge[]) entry.getValue()) {
                    if (hashSet.contains(badge4)) {
                        list.add(badge4);
                    }
                }
            }
        }
        HashMap hashMap2 = new HashMap();
        for (Object pair2 : hashMap.entrySet()) { Map.Entry entry2 = (Map.Entry) pair2;
            hashMap2.put((String) entry2.getKey(), (Badge[]) ((List) entry2.getValue()).toArray(new Badge[0]));
        }
        numbered = (Badge[]) arrayList.toArray(new Badge[0]);
        known = hashMap2;
        HashSet hashSet2 = new HashSet();
        for (Badge badge5 : numbered) {
            hashSet2.add(badge5.image);
        }
        Map<String, Bitmap> map = pictures;
        synchronized (map) {
            map.keySet().retainAll(hashSet2);
            pictureAsked.keySet().retainAll(hashSet2);
        }
    }

    private static String localised(JSONObject jSONObject, String str) {
        String optString = jSONObject.optString(str + "_" + Locale.getDefault().getLanguage(), "");
        return optString.length() > 0 ? optString : jSONObject.optString(str, "");
    }

    private static int colour(String str) {
        try {
            if (str.length() == 0) {
                return 0;
            }
            return Integer.parseInt(str.replace("#", ""), 16) | (-16777216);
        } catch (Throwable unused) {
            return 0;
        }
    }

    private static File file(Context context) {
        return file(context, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static File file(Context context, boolean z) {
        return new File(context.getFilesDir(), z ? "ttcuz/margyt-badges.json" : "ttcuz/badges.json");
    }

    public static Bitmap picture(Context context, final String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        if ("tty:supporter".equals(str)) {
            return TtyBadges.picture(context);
        }
        final String pictureAddress = pictureAddress(str);
        if (pictureAddress == null) {
            return null;
        }
        Map<String, Bitmap> map = pictures;
        synchronized (map) {
            if (map.containsKey(str)) {
                Bitmap bitmap = map.get(str);
                Long l = pictureAsked.get(str);
                if (bitmap != null || (l != null && SystemClock.uptimeMillis() - l.longValue() < 30000)) {
                    return bitmap;
                }
            }
            map.put(str, null);
            pictureAsked.put(str, Long.valueOf(SystemClock.uptimeMillis()));
            final File file = new File(context.getFilesDir(), "ttcuz/badges/" + name(str));
            byte[] read = Net.read(file);
            if (read != null) {
                Bitmap remember = remember(str, read);
                if (remember != null) {
                    return remember;
                }
                file.delete();
            }
            Net.away("badge picture", new Runnable() { // from class: cat.ttcuz.framework.Badges.3
                @Override // java.lang.Runnable
                public void run() {
                    byte[] bytes = Net.bytes(pictureAddress);
                    if (bytes == null || Badges.remember(str, bytes) == null) {
                        return;
                    }
                    Net.save(file, bytes);
                    cat.ttcuz.framework.Badge.refreshProfiles();
                    Badges.told();
                }
            });
            return null;
        }
    }

    static String pictureAddress(String str) {
        if (str == null) {
            return null;
        }
        if (str.startsWith(MARGYT_IMAGES)) {
            String substring = str.substring(7);
            if (!substring.matches("[A-Za-z0-9_-]{1,64}\\.png(?:\\?v=[a-f0-9]{1,64})?")) {
                return null;
            }
            return "https://margy.narez.xyz/api/icon/" + substring;
        }
        if (!str.matches("badges/[a-z0-9_-]{1,32}\\.png")) {
            return null;
        }
        return FILES + str;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Bitmap remember(String str, byte[] bArr) {
        if (bArr.length > 262144) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options);
        if (options.outWidth > 0 && options.outHeight > 0 && options.outWidth <= 1024 && options.outHeight <= 1024) {
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = 1;
            while (Math.max(options.outWidth, options.outHeight) / options2.inSampleSize > MOST) {
                options2.inSampleSize *= 2;
            }
            Bitmap decodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length, options2);
            if (decodeByteArray == null) {
                return null;
            }
            Bitmap crop = cat.ttcuz.framework.Badge.crop(decodeByteArray);
            Map<String, Bitmap> map = pictures;
            synchronized (map) {
                map.put(str, crop);
            }
            return crop;
        }
        return null;
    }

    private static String name(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char charAt = str.charAt(i);
            if (!Character.isLetterOrDigit(charAt) && charAt != '.') {
                charAt = '_';
            }
            sb.append(charAt);
        }
        return sb.toString();
    }
}
