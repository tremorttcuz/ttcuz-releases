package cat.narezany.margyt;
/** Inclusive visual bounds: the last half of an image span is still tappable. */
final class BadgeHit {
    static boolean contains(float x,float y,float left,float right,float top,float bottom) {
        return right>left && bottom>top && x>=left && x<=right && y>=top && y<=bottom;
    }
}
