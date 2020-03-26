package com.e4d.pipeline

import com.e4d.build.TextValueResolver

/**
 * A text value resolver uses a pipeline to resolve values
 * that might be stored as credentials.
 */
class PipelineTextValueResolver implements TextValueResolver {
  def pipeline

  /**
   * Constructor
   *
   * @param pipeline A reference to a pipeline job script.
   */
  PipelineTextValueResolver(pipeline) {
    this.pipeline = pipeline
  }

  /**
   * See {@code @TextValueResolver}
   */
  String resolve(String qualifier) {
    if (qualifier?.startsWith('#')) {
      qualifier = qualifier[1..-1]
      def creds = [pipeline.string(credentialsId: qualifier, variable: qualifier)]
      pipeline.withCredentials(creds) {
        pipeline.env[qualifier]
      }
    } else {
      qualifier
    }
  }
}
