package net.floodlightcontroller.routing;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.floodlightcontroller.routing.web.serializers.VirtualGatewaySerializer;
import org.projectfloodlight.openflow.types.MacAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Qing Wang (qw@g.clemson.edu) at 12/20/17
 */
@JsonSerialize(using = VirtualGatewaySerializer.class)
public class VirtualGateway {
    private volatile String name;
    private volatile MacAddress gatewayMac;
    private volatile ArrayList<VirtualGatewayInterface> interfaces;

    protected static Logger log = LoggerFactory.getLogger(VirtualGateway.class);

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public VirtualGateway(@JsonProperty("gateway-name") String name, @JsonProperty("gateway-mac") String mac) {
        this.name = name;
        this.gatewayMac = MacAddress.of(mac);
        this.interfaces = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public MacAddress getGatewayMac() {
        return gatewayMac;
    }

    public List<VirtualGatewayInterface> getInterfaces() {
        return interfaces;
    }

    public Optional<VirtualGatewayInterface> getInterface(String name) {
        return interfaces.stream()
                .filter(intf -> intf.getInterfaceName().equals(name))
                .findAny();
    }

    public void setGatewayMac(MacAddress mac) {
        this.gatewayMac = mac;
    }

    public void addInterface(VirtualGatewayInterface vInterface) {
        if (!interfaces.contains(vInterface)) {
            interfaces.add(vInterface);
        }else {
            interfaces.set(interfaces.indexOf(vInterface), vInterface);
        }
    }

    public void removeInterface(VirtualGatewayInterface vInterface) {
        interfaces.remove(vInterface);
    }

    public void clearInterfaces() {
        this.interfaces.clear();
    }

    public void updateInterface(VirtualGatewayInterface vInterface) {
        VirtualGatewayInterface intf = getInterface(vInterface.getInterfaceName()).get();
        intf.setIp(vInterface.getIp());
        intf.setMac(vInterface.getMac());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VirtualGateway that = (VirtualGateway) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (gatewayMac != null ? !gatewayMac.equals(that.gatewayMac) : that.gatewayMac != null) return false;
        return interfaces != null ? interfaces.equals(that.interfaces) : that.interfaces == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (gatewayMac != null ? gatewayMac.hashCode() : 0);
        result = 31 * result + (interfaces != null ? interfaces.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "VirtualGateway{" +
                "name='" + name + '\'' +
                ", gatewayMac=" + gatewayMac +
                ", interfaces=" + interfaces +
                '}';
    }
}
