package Com.app.cuts.plows.Models;

public class Steps {
    private LocationModel start_locationModel;
    private LocationModel end_locationModel;
    private OverviewPolyLine polyline;

    public LocationModel getStart_locationModel() {
        return start_locationModel;
    }

    public LocationModel getEnd_locationModel() {
        return end_locationModel;
    }

    public OverviewPolyLine getPolyline() {
        return polyline;
    }
}
