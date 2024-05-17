package ir.ramtung.tinyme.messaging.request;
import lombok.Data;

@Data
public class ChangeMatchingStateRq {
    private String securityIsin;
    private MatchingState targetState;

    public ChangeMatchingStateRq(String securityIsin, MatchingState matchingState){
        this.securityIsin = securityIsin;
        this.targetState = matchingState;
    }
}
