package com.e4d.job.declaration

import com.e4d.job.IntegrateJob
import com.e4d.pipeline.DummyPipeline
import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*
import static org.mockito.hamcrest.MockitoHamcrest.*
import static org.mockito.Mockito.*

class IntegrateJobDeclarationTest {
  final pipeline = mock(DummyPipeline)
  final job = spy(new IntegrateJob(pipeline))
  final decl = new IntegrateJobDeclaration(job)

  @Test void rename_me() {
    [
      {
        artifact {
          baseName 'base name'
        }
      },

      {
        artifact.baseName 'base name'
      }
    ].each { declaration ->
      // Arrange
      job.artifactBaseName = ''
      // Act
      decl.define(declaration)
      // Assert
      assertThat(job.artifactBaseName, is(equalTo('base name')))
    }
  }
}
