public class Pair<K, V> {
    private final K left;
    private final V right;

    public Pair(K left, V right) {
        this.left = left;
        this.right = right;
    }

    public static <T, G> Pair<T, G> of(T left, G right) {
        return new Pair<>(left, right);
    }

    public K getLeft() {
        return left;
    }

    public V getRight() {
        return right;
    }
}