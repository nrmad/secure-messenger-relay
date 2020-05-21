package networking;

import java.util.HashMap;
import java.util.Map;

public enum Type {
    MESSAGE(0),
    SEND_FAILED(2),
    END_SESSION(3),
    ACCEPT_USER(4),
    REQUEST_USER(5);

    private final int code;
    private static Map<Integer,Type> map = new HashMap<>();

    Type(int code){
        this.code = code;
    }

    static {
        for(Type type : Type.values()){
            map.put(type.code, type);
        }
    }

    public static Type valueOf(int code){
        return map.get(code);
    }

    public int getCode(){
        return code;
    }

}
