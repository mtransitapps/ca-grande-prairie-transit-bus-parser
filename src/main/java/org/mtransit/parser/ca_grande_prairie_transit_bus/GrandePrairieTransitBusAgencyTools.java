package org.mtransit.parser.ca_grande_prairie_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// https://data.cityofgp.com/Transportation/GP-Transit-GTFS-Feed/kwef-vsek
// https://data.cityofgp.com/download/kwef-vsek/ZIP
// http://jump.nextinsight.com/gtfs/ni_gp/google_transit.zip
// https://gpt.mapstrat.com/current/google_transit.zip
public class GrandePrairieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-grande-prairie-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new GrandePrairieTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating GP Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating GP Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
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

	private static final Pattern STARTS_WITH_ROUTE_ = Pattern.compile("(^route )", Pattern.CASE_INSENSITIVE);

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		String rsn = gRoute.getRouteShortName();
		rsn = STARTS_WITH_ROUTE_.matcher(rsn).replaceAll(EMPTY);
		return rsn;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
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

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Country Club") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("151"), // 63Ave-98St
								Stops.getALL_STOPS().get("159"), // 72Ave-99St
								Stops.getALL_STOPS().get("M7W"), // Towne Centre
								Stops.getALL_STOPS().get("M4") // Prairie Mall
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("M4"), // Prairie Mall
								Stops.getALL_STOPS().get("M7N"), // Towne Centre
								Stops.getALL_STOPS().get("151") // 63Ave-98St
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Poplar Dr") // COUNTRYSIDE SOUTH
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("221"), // Poplar Dr 74Ave
								Stops.getALL_STOPS().get("243"), // 62Ave-90St #CountrysideSouth
								Stops.getALL_STOPS().get("M7W"), // Towne Centre
								Stops.getALL_STOPS().get("M4") // Prairie Mall
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("M4"), // Prairie Mall
								Stops.getALL_STOPS().get("M7N"), // Towne Centre
								Stops.getALL_STOPS().get("221") // Poplar Dr 74Ave
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Mission Hts") // EASTLINK CENTRE
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("321"), // MHD-83Ave
								Stops.getALL_STOPS().get("M7N"), // Towne Centre
								Stops.getALL_STOPS().get("M4") // Prairie Mall
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("M4"), // Prairie Mall
								Stops.getALL_STOPS().get("M7W"), // Towne Centre
								Stops.getALL_STOPS().get("321") // MHD-83Ave
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Lakeland Dr", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "GPRC") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("M6"), // GPRC
								Stops.getALL_STOPS().get("534"), // != 121Ave-100St
								Stops.getALL_STOPS().get("M2"), // <> Greyhound
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
								Stops.getALL_STOPS().get("503"), // != 100St-121Ave
								Stops.getALL_STOPS().get("533") // Lakeland Dr 123Ave
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("533"), // Lakeland Dr 123Ave
								Stops.getALL_STOPS().get("591"), // != 97St-117Ave
								Stops.getALL_STOPS().get("M2"), // <> Greyhound
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
								Stops.getALL_STOPS().get("554"), // != 121Ave-102St
								Stops.getALL_STOPS().get("M6") // GPRC
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "GPRC", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Westpointe") // EASTLINK CENTRE
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("603"), // Westpointe 83Ave
								Stops.getALL_STOPS().get("605"), "157", // ++ Westpointe Dr-81 Ave
								Stops.getALL_STOPS().get("629"), "162", // ++ Pinnacle Dr-Pinnacle Way
								Stops.getALL_STOPS().get("M9"), // Eastlink Centre
								Stops.getALL_STOPS().get("639"), "663", // ++ 106 St-73 Ave
								Stops.getALL_STOPS().get("608"), "669", // ++ Industrial Training Centre
								Stops.getALL_STOPS().get("M6") // GPRC
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("M6"), // GPRC
								Stops.getALL_STOPS().get("614"), "670", // ++ 107 Ave-109 St
								Stops.getALL_STOPS().get("662"), "212", // 84 Ave-113 St
								Stops.getALL_STOPS().get("603") // Westpointe 83Ave / Fas Gas on WestPointe Dr
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(9L, new RouteTripSpec(9L, // SJS
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Prairie Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St Joes HS") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("335"), // St. Joes HS
								Stops.getALL_STOPS().get("M4") // <> Prairie Mall
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								Stops.getALL_STOPS().get("533"), // Lakeland Dr 123Ave
								Stops.getALL_STOPS().get("M4"), // <> Prairie Mall
								Stops.getALL_STOPS().get("335") // St. Joes HS
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(92L, new RouteTripSpec(92L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Hythe", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Towne Ctr Mall") // Grande Prairie
				.addTripSort(0, //
						Arrays.asList( //
								Stops.getALL_STOPS().get("9298"), // City Service Center on 97 Ave #GrandePrairie
								Stops.getALL_STOPS().get("9212") // 103 St across from Tags #Hythe
						)) //
				.addTripSort(1, //
						Arrays.asList( //
								Stops.getALL_STOPS().get("9306"), // 4 Ave at Amisk Court #Beaverlodge
								Stops.getALL_STOPS().get("9212"), // 103 St across from Tags #Hythe
								Stops.getALL_STOPS().get("9298") // City Service Center on 97 Ave #GrandePrairie
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final int directionId = gTrip.getDirectionIdOrDefault();
		final String tripHeadsign = gTrip.getTripHeadsignOrDefault();
		if (mRoute.getId() == 4L) {
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("Downtown / GPRC / Costco", 0);
				return;
			}
		} else if (mRoute.getId() == 7L) {
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("Eastlink / O’Brien Lk / Signature Falls", 0);
				return;
			}
		} else if (mRoute.getId() == 8L) { // SJP
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("St John Paul II", 0);
				return;
			}
		} else if (mRoute.getId() == 90L) {
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("0 Clairmont", 0);
				return;
			} else if (directionId == 1 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("1 Clairmont", 1);
				return;
			}
		} else if (mRoute.getId() == 91L) {
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("0 Sexsmith", 0);
				return;
			} else if (directionId == 1 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("1 Sexsmith", 1);
				return;
			}
		} else if (mRoute.getId() == 92L) {
			if (directionId == 0 //
					&& (StringUtils.isEmpty(tripHeadsign) || "3 Downtown via Library/QEII".equals(tripHeadsign))) {
				mTrip.setHeadsignString("0 3 Downtown via Library/QEI", 0);
				return;
			} else if (directionId == 1 //
					&& (StringUtils.isEmpty(tripHeadsign) || "3 Downtown via Library/QEII".equals(tripHeadsign))) {
				mTrip.setHeadsignString("1 3 Downtown via Library/QEI", 1);
				return;
			}
		} else if (mRoute.getId() == 93L) {
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("0 Beaverlodge", 0);
				return;
			} else if (directionId == 1 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("1 Beaverlodge", 1);
				return;
			}
		} else if (mRoute.getId() == 94L) {
			if (directionId == 0 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("0 Wembley", 0);
				return;
			} else if (directionId == 1 && StringUtils.isEmpty(tripHeadsign)) {
				mTrip.setHeadsignString("1 Wembley", 1);
				return;
			}
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(tripHeadsign),
				directionId
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // BECAUSE provided direction_id useless & no trip head-sign
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypesFRCA(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern DASH_ = Pattern.compile("(^([^-]+)-([^-]+)$)", Pattern.CASE_INSENSITIVE);
	private static final String DASH_REPLACEMENT = "$2 - $3";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = DASH_.matcher(gStopName).replaceAll(DASH_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
