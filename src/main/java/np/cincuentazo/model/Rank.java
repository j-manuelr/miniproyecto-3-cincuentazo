package np.cincuentazo.model;

public enum Rank {


    TWO ("2", 2),
    THREE ("3", 3),
    FOUR ("4", 4),
    FIVE ("5", 5),
    SIX ("6", 6),
    SEVEN ("7", 7),
    EIGHT ("8", 8),
    NINE ("9", 0),
    TEN ("10", 10),
    JACK ("J", -10),
    QUEEN ("Q", -10),
    KING ("K", -10),
    ACE ("A", Constants.ACE_FLEXIBLE);

    private static final class Constants{
        static  final int ACE_FLEXIBLE = Integer.MIN_VALUE;
    }

    public static final int ACE_FLEXIBLE = Constants.ACE_FLEXIBLE;

    private final String label;

    private final int baseValue;

    Rank(String label, int baseValue){
        this.label = label;
        this.baseValue = baseValue;
    }

    public String getLabel() {
        return label;
    }

    public int getBaseValue() {
        return baseValue;
    }

    public boolean isAce(){
        return  this == ACE;
    }
    @Override
    public String toString() {
        if (isAce()) return label + "(flexible)";
        return label + "(" + baseValue + ")";
    }
}

