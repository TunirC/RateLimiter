package Core.Resolver;

import java.util.List;

public interface KeyResolver {
    List<String> resolveKey(String userId, String ip);
}
