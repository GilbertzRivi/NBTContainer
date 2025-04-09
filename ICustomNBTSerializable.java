package net.oktawia.crazyae2addons.interfaces;

public interface ICustomNBTSerializable {
    static final String SEP = "|";

    String serialize();
    void deserialize(String data);
}
