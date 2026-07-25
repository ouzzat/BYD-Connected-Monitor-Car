package ma.asmontec.bydmonitor.mobile.data;

/**
 * Dernier état connu du véhicule, tel que reçu par MQTT. Tout champ non
 * encore reçu (ou explicitement {@code null} dans le message d'origine)
 * reste {@code null} : l'interface doit afficher "—", jamais une valeur
 * inventée.
 */
public final class VehicleState {

    public Boolean online;
    public long lastUpdateAt;

    public Double speedKmh;
    public Double latitude;
    public Double longitude;
    public Double altitude;
    public Double accuracy;

    public Integer androidBattery;
    public String networkType;
    public Integer signalLevel;

    public Double soc;
    public Double rangeKm;
    public Double powerKw;
    public Double torqueNm;
    public Double batteryTemp;

    public VehicleState copy() {
        VehicleState c = new VehicleState();
        c.online = online;
        c.lastUpdateAt = lastUpdateAt;
        c.speedKmh = speedKmh;
        c.latitude = latitude;
        c.longitude = longitude;
        c.altitude = altitude;
        c.accuracy = accuracy;
        c.androidBattery = androidBattery;
        c.networkType = networkType;
        c.signalLevel = signalLevel;
        c.soc = soc;
        c.rangeKm = rangeKm;
        c.powerKw = powerKw;
        c.torqueNm = torqueNm;
        c.batteryTemp = batteryTemp;
        return c;
    }
}
