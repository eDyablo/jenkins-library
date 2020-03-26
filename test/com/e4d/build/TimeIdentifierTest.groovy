package com.e4d.build

import org.junit.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.mockito.Mockito.*

class TimeIdentifierTest extends TimeIdentifier {
  final Calendar calendar = mock(Calendar.class)

  @Override Calendar getTime() {
    calendar
  }

  @Test void identity_is_properly_constructed_from_year_day_of_year_and_time_with_seconds_precision() {
    when(calendar.get(Calendar.YEAR)).thenReturn(1)
    when(calendar.get(Calendar.DAY_OF_YEAR)).thenReturn(2)
    when(calendar.get(Calendar.HOUR_OF_DAY)).thenReturn(3)
    when(calendar.get(Calendar.MINUTE)).thenReturn(4)
    when(calendar.get(Calendar.SECOND)).thenReturn(5)
    assertThat(identity, is(equalTo('0001002030405')))
  }
}
