package Core.Resolver;

import java.util.ArrayList;
import java.util.List;

public class HybridKeyResolver implements KeyResolver {
    @Override
    public List<String> resolveKey(String userId, String ip) {
        List<String> keys = new ArrayList<>();

        if (!userId.isEmpty() && !userId.isBlank()) keys.add("USER_ID: "+userId);

        if (!ip.isEmpty() && !ip.isBlank()) keys.add("IP: "+ip);

        //keys.add("GLOBAL: "+100);

        return keys;
    }
}
