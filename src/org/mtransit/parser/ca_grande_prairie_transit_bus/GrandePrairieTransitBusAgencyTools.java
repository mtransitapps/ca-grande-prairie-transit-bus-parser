package org.mtransit.parser.ca_grande_prairie_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://data.cityofgp.com/Transportation/GP-Transit-GTFS-Feed/kwef-vsek
// https://data.cityofgp.com/download/kwef-vsek/ZIP
// http://jump.nextinsight.com/gtfs/ni_gp/google_transit.zip
// https://gpt.mapstrat.com/current/google_transit.zip
public class GrandePrairieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-grande-prairie-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new GrandePrairieTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating GP Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating GP Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			if ("SJP".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return 8L;
			} else if ("SJS".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return 9L;
			}
			System.out.printf("\nUnexpected route id %s!\n", gRoute);
			System.exit(-1);
			return -1L;
		}
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		if (("Route " + gRoute.getRouteShortName()).equals(routeLongName)) {
			routeLongName = null;
		}
		if (StringUtils.isEmpty(routeLongName)) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return "Downtown / Southview & Country Club / Prairie Mall";
			case 2: return "Downtown / Prairie Mall / Popular Dr & Countryside S";
			case 3: return "Downtown / Prairie Mall / Eastlink Ctr";
			case 4: return "Downtown / GPRC / Costco";
			case 5: return "Lakeland & Crystal Lk / Prairie Mall / Royal Oaks & GPRC";
			case 6: return "Westgate & Ctr W / Westpoite / Eastlink Ctr / GPRC";
			case 7: return "Countryside S / Eastlink / O'Brien Lk / Signature Falls";
			case 90: return gRoute.getRouteLongName(); // TODO?
			case 91: return gRoute.getRouteLongName(); // TODO?
			// @formatter:on
			}
			System.out.printf("\nUnexpected route long name %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteLongName(gRoute);
	}

	private static final String AGENCY_COLOR_GREEN = "056839"; // GREEN (from PNG logo)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Country Club") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("151"), Stops2.ALL_STOPS2.get("151"), // 63Ave-98St
								Stops.ALL_STOPS.get("159"), Stops2.ALL_STOPS2.get("159"), // 72Ave-99St
								Stops.ALL_STOPS.get("M7W"), Stops2.ALL_STOPS2.get("M7W"), // Towne Centre
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // Prairie Mall
								Stops.ALL_STOPS.get("M7N"), Stops2.ALL_STOPS2.get("M7N"), // Towne Centre
								Stops.ALL_STOPS.get("151"), Stops2.ALL_STOPS2.get("151"), // 63Ave-98St
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Poplar Dr") // COUNTRYSIDE SOUTH
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("221"), Stops2.ALL_STOPS2.get("221"), // Poplar Dr 74Ave
								Stops.ALL_STOPS.get("243"), Stops2.ALL_STOPS2.get("243"), // 62Ave-90St #CountrysideSouth
								Stops.ALL_STOPS.get("M7W"), Stops2.ALL_STOPS2.get("M7W"), // Towne Centre
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // Prairie Mall
								Stops.ALL_STOPS.get("M7N"), Stops2.ALL_STOPS2.get("M7N"), // Towne Centre
								Stops.ALL_STOPS.get("221"), Stops2.ALL_STOPS2.get("221"), // Poplar Dr 74Ave
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Mission Hts") // EASTLINK CENTRE
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("321"), Stops2.ALL_STOPS2.get("321"), // MHD-83Ave
								Stops.ALL_STOPS.get("M7N"), Stops2.ALL_STOPS2.get("M7N"), // Towne Centre
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // Prairie Mall
								Stops.ALL_STOPS.get("M7W"), Stops2.ALL_STOPS2.get("M7W"), // Towne Centre
								Stops.ALL_STOPS.get("321"), Stops2.ALL_STOPS2.get("321"), // MHD-83Ave
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Crystal Lk", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "GPRC") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("M6"), Stops2.ALL_STOPS2.get("M6"), // GPRC
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // <> Prairie Mall
								Stops.ALL_STOPS.get("551"), Stops2.ALL_STOPS2.get("551"), // Crystal LkDr 128Ave
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("551"), Stops2.ALL_STOPS2.get("551"), // Crystal LkDr 128Ave
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // <> Prairie Mall
								Stops.ALL_STOPS.get("M6"), Stops2.ALL_STOPS2.get("M6"), // GPRC
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "GPRC", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Westpointe") // EASTLINK CENTRE
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("603"), Stops2.ALL_STOPS2.get("603"), // Westpointe 83Ave
								Stops.ALL_STOPS.get("M9"), Stops2.ALL_STOPS2.get("M9"), // Eastlink Centre
								Stops.ALL_STOPS.get("M6"), Stops2.ALL_STOPS2.get("M6"), // GPRC
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("M6"), Stops2.ALL_STOPS2.get("M6"), // GPRC
								Stops.ALL_STOPS.get("638"), Stops2.ALL_STOPS2.get("638"), // ++ 97Ave-119St
								Stops.ALL_STOPS.get("603"), Stops2.ALL_STOPS2.get("603"), // Westpointe 83Ave
						})) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, // SJP
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St John Paul II", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St Joes HS") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("335A"), Stops2.ALL_STOPS2.get("335A"), // St Joes High School
								Stops.ALL_STOPS.get("335J"), Stops2.ALL_STOPS2.get("335J"), // St John Paul II School
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("335J"), Stops2.ALL_STOPS2.get("335J"), // St John Paul II School
								Stops.ALL_STOPS.get("335A"), Stops2.ALL_STOPS2.get("335A"), // St Joes High School
						})) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, // SJS
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St Joes HS") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("335"), Stops2.ALL_STOPS2.get("335"), // St. Joes HS
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // <> Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.ALL_STOPS.get("533"), Stops2.ALL_STOPS2.get("533"), // Lakeland Dr 123Ave
								Stops.ALL_STOPS.get("M4"), Stops2.ALL_STOPS2.get("M4"), // <> Prairie Mall
								Stops.ALL_STOPS.get("335"), Stops2.ALL_STOPS2.get("335"), // St. Joes HS
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (isGoodEnoughAccepted()) {
			if (mRoute.getId() == 4L) {
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("Downtown / GPRC / Costco", 0);
					return;
				}
			} else if (mRoute.getId() == 7L) {
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("Eastlink / O’Brien Lk / Signature Falls", 0);
					return;
				}
			} else if (mRoute.getId() == 90L) {
				if (false) {
					// TODO
				}
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("0", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("1", 1);
					return;
				}
			} else if (mRoute.getId() == 91L) {
				if (false) {
					// TODO
				}
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("0", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("1", 1);
					return;
				}
			}
		}
		String tripHeadsign = gTrip.getTripHeadsign();
		int directionId = gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId();
		mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), directionId);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		System.out.printf("\nUnexptected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
