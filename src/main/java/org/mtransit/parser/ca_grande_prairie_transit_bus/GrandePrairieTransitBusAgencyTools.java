package org.mtransit.parser.ca_grande_prairie_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
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
		MTLog.log("Generating GP Transit bus data...");
		long start = System.currentTimeMillis();
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		if (isNext) {
			setupNext();
		}
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating GP Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	private void setupNext() {
		// DO NOTHING
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
		String rsn = getRouteShortName(gRoute);
		if (!Utils.isDigitsOnly(rsn)) {
			if ("SJP".equalsIgnoreCase(rsn)) {
				return 8L;
			} else if ("SJS".equalsIgnoreCase(rsn)) {
				return 9L;
			}
			throw new MTLog.Fatal("Unexpected route id %s!", gRoute);
		}
		return Long.parseLong(rsn); // use route short name as route ID
	}

	private static final Pattern STARTS_WTHI_ROUTE_ = Pattern.compile("(^route )", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteShortName(GRoute gRoute) {
		String rsn = gRoute.getRouteShortName();
		rsn = STARTS_WTHI_ROUTE_.matcher(rsn).replaceAll(StringUtils.EMPTY);
		return rsn;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		String rsn = getRouteShortName(gRoute);
		if (("Route " + rsn).equals(routeLongName)) {
			routeLongName = null;
		}
		if (StringUtils.isEmpty(routeLongName)) {
			switch (Integer.parseInt(rsn)) {
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
			case 92: return gRoute.getRouteLongName(); // TODO?
			case 93: return gRoute.getRouteLongName(); // TODO?
			case 94: return gRoute.getRouteLongName(); // TODO?
			// @formatter:on
			}
			throw new MTLog.Fatal("Unexpected route long name %s!", gRoute);
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
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Country Club") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("151"), // 63Ave-98St
								Stops.getALL_STOPS().get("159"), // 72Ave-99St
								Stops.getALL_STOPS().get("M7W"), // Towne Centre
								Stops.getALL_STOPS().get("M4"), // Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("M4"), // Prairie Mall
								Stops.getALL_STOPS().get("M7N"), // Towne Centre
								Stops.getALL_STOPS().get("151"), // 63Ave-98St
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Poplar Dr") // COUNTRYSIDE SOUTH
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("221"), // Poplar Dr 74Ave
								Stops.getALL_STOPS().get("243"), // 62Ave-90St #CountrysideSouth
								Stops.getALL_STOPS().get("M7W"), // Towne Centre
								Stops.getALL_STOPS().get("M4"), // Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("M4"), // Prairie Mall
								Stops.getALL_STOPS().get("M7N"), // Towne Centre
								Stops.getALL_STOPS().get("221"), // Poplar Dr 74Ave
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Mission Hts") // EASTLINK CENTRE
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("321"), // MHD-83Ave
								Stops.getALL_STOPS().get("M7N"), // Towne Centre
								Stops.getALL_STOPS().get("M4"), // Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("M4"), // Prairie Mall
								Stops.getALL_STOPS().get("M7W"), // Towne Centre
								Stops.getALL_STOPS().get("321"), // MHD-83Ave
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Lakeland Dr", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "GPRC") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("M6"), // GPRC
								Stops.getALL_STOPS().get("534"), // != 121Ave-100St
								Stops.getALL_STOPS().get("M2"), // <> Greyhound
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
								Stops.getALL_STOPS().get("503"), // != 100St-121Ave
								Stops.getALL_STOPS().get("533"), // Lakeland Dr 123Ave

						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("533"), // Lakeland Dr 123Ave
								Stops.getALL_STOPS().get("591"), // != 97St-117Ave
								Stops.getALL_STOPS().get("M2"), // <> Greyhound
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
								Stops.getALL_STOPS().get("554"), // != 121Ave-102St
								Stops.getALL_STOPS().get("M6"), // GPRC
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "GPRC", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Westpointe") // EASTLINK CENTRE
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("603"), // ?? Westpointe 83Ave
								Stops.getALL_STOPS().get("605"), // ++ Westpointe 81Ave
								Stops.getALL_STOPS().get("607"), // ++ Westpointe 77Ave
								"205", "206", "207", "208", "209", "210", "211", "212", "213", // !=
								Stops.getALL_STOPS().get("629"), // Pinnacle Dr Pinnacle Way
								Stops.getALL_STOPS().get("M9"), // Eastlink Centre
								"215", "216", "217", // !=
								Stops.getALL_STOPS().get("664"), // ++ 109St-86Ave
								Stops.getALL_STOPS().get("668"), // ?? by St. John's Ambulance (on 109 St)
								Stops.getALL_STOPS().get("670"), // ++ Westside Dr 107St
								Stops.getALL_STOPS().get("M6"), // GPRC
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("M6"), // GPRC
								Stops.getALL_STOPS().get("632"), // Service Canada / Canadian Tire (on 101 Ave)
								Stops.getALL_STOPS().get("642"), // 97Ave-117St / Winners (on 97 Ave)
								Stops.getALL_STOPS().get("662"), // ++ 84Ave-113St
								Stops.getALL_STOPS().get("603"), // ?? Westpointe 83Ave / Fas Gas on WestPointe Dr
						})) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, // SJS
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St Joes HS") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("335"), // St. Joes HS
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("533"), // Lakeland Dr 123Ave
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
								Stops.getALL_STOPS().get("335"), // St. Joes HS
						})) //
				.compileBothTripSort());
		map2.put(92L, new RouteTripSpec(92L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Hythe", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Towne Ctr Mall") // Grande Prairie
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("9298"), // City Service Center on 97 Ave #GrandePrairie
								Stops.getALL_STOPS().get("9212"), // 103 St across from Tags #Hythe
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						Stops.getALL_STOPS().get("9306"), // 4 Ave at Amisk Court #Beaverlodge
								Stops.getALL_STOPS().get("9212"), // 103 St across from Tags #Hythe
								Stops.getALL_STOPS().get("9298"), // City Service Center on 97 Ave #GrandePrairie
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
		// if (isGoodEnoughAccepted()) {
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
			} else if (mRoute.getId() == 8L) { // SJP
					if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
						mTrip.setHeadsignString("St John Paul II", 0);
						return;
					}
			} else if (mRoute.getId() == 90L) {
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("0 Clairmont", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("1 Clairmont", 1);
					return;
				}
			} else if (mRoute.getId() == 91L) {
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("0 Sexsmith", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("1 Sexsmith", 1);
					return;
				}
			} else if (mRoute.getId() == 92L) {
				if (gTrip.getDirectionId() == 0 //
						&& (StringUtils.isEmpty(gTrip.getTripHeadsign()) || "3 Downtown via Library/QEII".equals(gTrip.getTripHeadsign()))) {
					mTrip.setHeadsignString("0 3 Downtown via Library/QEI", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 //
						&& (StringUtils.isEmpty(gTrip.getTripHeadsign()) || "3 Downtown via Library/QEII".equals(gTrip.getTripHeadsign()))) {
					mTrip.setHeadsignString("1 3 Downtown via Library/QEI", 1);
					return;
				}
			} else if (mRoute.getId() == 93L) {
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("0 Beaverlodge", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("1 Beaverlodge", 1);
					return;
				}
			} else if (mRoute.getId() == 94L) {
				if (gTrip.getDirectionId() == 0 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("0 Wembley", 0);
					return;
				} else if (gTrip.getDirectionId() == 1 && StringUtils.isEmpty(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString("1 Wembley", 1);
					return;
				}
			}
		// }
		String tripHeadsign = gTrip.getTripHeadsign();
		int directionId = gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId();
		mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), directionId);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexptected trips to merge %s & %s!", mTrip, mTripToMerge);
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
