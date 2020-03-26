package com.e4d.build

/**
 * Identifier that defines common behavior for identifiers using time
 * to construct identity.
 */
abstract class TimeIdentifier implements Identifier {
  static final String paddingSymbol = '0'

  @Override String getIdentity() {
    def calendar = getTime()
    calendar.get(Calendar.YEAR).toString().padLeft(4, paddingSymbol) <<
    calendar.get(Calendar.DAY_OF_YEAR).toString().padLeft(3, paddingSymbol) <<
    calendar.get(Calendar.HOUR_OF_DAY).toString().padLeft(2, paddingSymbol) <<
    calendar.get(Calendar.MINUTE).toString().padLeft(2, paddingSymbol) <<
    calendar.get(Calendar.SECOND).toString().padLeft(2, paddingSymbol)
  }

  protected abstract Calendar getTime()
}
