package core;

public class BindingPower {
    public int left;
    public int right;

    public BindingPower(int left, int right) {
        this.left = left;
        this.right = right;
    }

    public BindingPower(BinOp op) {
        switch (op) {
            case Add, Sub -> {
                this.left = 4;
                this.right = 5;
            }
            case Mul, Div, Mod -> {
                this.left = 6;
                this.right = 7;
            }
            case LT, GT, EQ, NEQ -> {
                this.left = 2;
                this.right = 3;
            }
            case And, Or -> {
                this.left = 0;
                this.right = 1;
            }
        }
    }
}

enum BinOp {
    Add, Sub, Mul, Div,

    LT, GT, EQ, NEQ,

    And, Or,

    Mod,
}