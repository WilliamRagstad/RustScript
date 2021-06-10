package core;
public class PrefixBindingPower {
    public int right;

    public PrefixBindingPower(int right) {
        this.right = right;
    }

    public PrefixBindingPower(PrefixOp op) throws Exception {
        switch (op) {
            case Negate, Head, Tail -> this.right = 10;
        };
    }
}

enum PrefixOp {
    Negate,

    Head, Tail,
}