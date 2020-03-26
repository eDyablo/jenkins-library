package com.e4d.secret

import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class SecretTextBearerTest {
  final pipeline = spy(DummyPipeline)

  @Test void gets_text_from_credentials_when_converted_to_string() {
    doReturn('secret creds').when(pipeline).string(
      argThat(hasEntry('credentialsId', 'secret'))
    )
    doReturn('secret text').when(pipeline).withCredentials(
      argThat(hasItem('secret creds')),
      argThat(any(Closure))
    )
    final bearer = new SecretTextBearer('secret')
    assertThat(bearer.declassify(pipeline), is(equalTo('secret text')))
  }
}
