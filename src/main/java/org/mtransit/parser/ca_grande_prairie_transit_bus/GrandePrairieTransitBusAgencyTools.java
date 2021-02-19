package org.mtransit.parser.ca_grande_prairie_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.mt.data.MAgency;

import java.util.regex.Pattern;

import static org.mtransit.commons.StringUtils.EMPTY;

// https://data.cityofgp.com/Transportation/GP-Transit-GTFS-Feed/kwef-vsek
// https://data.cityofgp.com/download/kwef-vsek/ZIP
// http://jump.nextinsight.com/gtfs/ni_gp/google_transit.zip
// https://gpt.mapstrat.com/current/google_transit.zip
public class GrandePrairieTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new GrandePrairieTransitBusAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Grande Prairie Transit";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		String rsn = getRouteShortName(gRoute);
		if (!CharUtils.isDigitsOnly(rsn)) {
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

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		String rsn = gRoute.getRouteShortName();
		rsn = STARTS_WITH_ROUTE_.matcher(rsn).replaceAll(EMPTY);
		return rsn;
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			String rsn = getRouteShortName(gRoute);
			return "Route " + rsn; // TODO route long name from directions head-sign?
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

	@Override
	public boolean directionSplitterEnabled() {
		return false; // not useful because all loops
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_P_ = Pattern.compile("( \\(.+\\)$)");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = ENDS_WITH_P_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
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
