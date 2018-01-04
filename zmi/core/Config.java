package core;

public class Config {
    public static int getAgentPort() {
        return Integer.parseInt(System.getenv("agent_port"));
    }

    public static int getClientPort() {
        return Integer.parseInt(System.getenv("client_port"));
    }

    public static int getSignerPort() {
        return Integer.parseInt(System.getenv("signer_port"));
    }

    public static int getGlobalNetworkServicePort() {
        return Integer.parseInt(System.getenv("global_network_service_port"));
    }
}
