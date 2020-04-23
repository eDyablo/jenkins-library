package com.e4d.job

class Option {
  enum When {
    ALWAYS,
    ON_SUCCESS,
    ON_FAILURE,
    ON_START,
    ON_COMPLETION,
  }

  enum Where {
    ANYWHERE,
  }
}
