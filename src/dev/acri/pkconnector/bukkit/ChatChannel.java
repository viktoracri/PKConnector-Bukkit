package dev.acri.pkconnector.bukkit;

public enum ChatChannel {
    NORMAL,GLOBAL,STAFF,VETERAN;

    public static ChatChannel get(String str){
        if(str.equalsIgnoreCase("NORMAL") || str.equalsIgnoreCase("N")) return NORMAL;
        else if(str.equalsIgnoreCase("GLOBAL") || str.equalsIgnoreCase("G")) return GLOBAL;
        else if(str.equalsIgnoreCase("STAFF") || str.equalsIgnoreCase("S")) return STAFF;
        else if(str.equalsIgnoreCase("VETERAN") || str.equalsIgnoreCase("V")) return VETERAN;
        return NORMAL;
    }

    public static ChatChannel getCanNull(String str){
        if(str.equalsIgnoreCase("NORMAL") || str.equalsIgnoreCase("N")) return NORMAL;
        else if(str.equalsIgnoreCase("GLOBAL") || str.equalsIgnoreCase("G")) return GLOBAL;
        else if(str.equalsIgnoreCase("STAFF") || str.equalsIgnoreCase("S")) return STAFF;
        else if(str.equalsIgnoreCase("VETERAN") || str.equalsIgnoreCase("V")) return VETERAN;
        return null;
    }
}
