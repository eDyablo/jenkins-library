package com.e4d.template

import org.junit.*
import static org.hamcrest.MatcherAssert.*
import static org.hamcrest.Matchers.*

class MapMergerTest {
  def merger = new MapMerger()

  @Test void merge_two_empty_maps_returns_empty_map() {    
    def result = merger.merge([:], [:])
    assertThat(result, is(equalTo([:])))
  }

  @Test void merge_single_element_map_with_empty_map_returns_single_element_map() {    
    def result = merger.merge([one:1], [:])
    assertThat(result, is(equalTo([one:1])))
  }

  @Test void merge_two_different_single_element_maps_returns_one_map_with_two_elements() {    
    def result = merger.merge([one:1], [two:2])
    assertThat(result, is(equalTo([one:1, two:2])))
  }

  @Test void merge_two_equal_single_element_maps_returns_one_map_with_one_element() {    
    def result = merger.merge([one:1], [one:1])
    assertThat(result, is(equalTo([one:1])))
  }

  @Test void merge_two_single_element_maps_with_equal_keys_and_different_values_returns_map_containing_second_value() {    
    def result = merger.merge([one:1], [one:2])
    assertThat(result, is(equalTo([one:2])))
  }

  @Test void merge_two_nested_maps_returns_nested_map_containing_second_value() {
    def first = [root:[one:1]]
    def second = [root:[one:2]]    
    def result = merger.merge(first, second)
    assertThat(result, is(equalTo([root:[one:2]])))
  }

  @Test void merge_more_then_two_nested_maps_returns_nested_map_containing_last_value() {
    def first = [root:[one:1]]
    def second = [root:[one:2]]
    def third = [root:[one:3]]  
    def result = merger.merge([first, second, third])
    assertThat(result, is(equalTo([root:[one:3]])))
  }

  @Test void merge_two_nested_maps_returns_nested_map_containing_existing_value_from_first_map() {
    def first = [root:[one:1, two:2]]
    def second = [root:[one:2]]     
    def result = merger.merge([first, second])
    assertThat(result, is(equalTo([root:[one:2, two:2]])))
  }
}
