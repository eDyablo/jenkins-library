package com.e4d.build

import java.util.Calendar

/**
 * A set of identifiers.
 */
class Identifiers {
  static final Identifier NO_IDENTITY = new NullIdentifier()

  static final Identifier UTC_TIME_IDENTITY = new UTCTimeIdentifier()

  static final Identifier LOCAL_TIME_IDENTITY = new LocalTimeIdentifier()
}

/**
 * Identifier that gives no identity.
 */
class NullIdentifier implements Identifier {
  String getIdentity() {}
}

/**
 * Identifier that gives identity build from current time in UTC.
 */
class UTCTimeIdentifier extends TimeIdentifier {
  @Override Calendar getTime() { new GregorianCalendar() }
}

/**
 * Identifier that gives identity build from current local time.
 */
class LocalTimeIdentifier extends TimeIdentifier {
  @Override Calendar getTime() { Calendar.instance }
}

/**
 * Identifier that gives identity build from specified epoch.
 */
class InstantEpochIdentifier extends TimeIdentifier {
  long seconds

  @Override Calendar getTime() {
    new Calendar.Builder().setInstant(seconds * 1000).build()
  }
}
