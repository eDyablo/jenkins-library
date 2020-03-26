package com.e4d.ioc

import com.e4d.pipeline.DummyPipeline

class DummyContext implements Context {
  final def _pipeline = new DummyPipeline()

  @Override
  def getPipeline() {
    _pipeline
  }
}
