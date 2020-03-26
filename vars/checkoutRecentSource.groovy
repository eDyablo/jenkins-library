import com.e4d.step.CheckoutRecentSourceStep
import com.e4d.ioc.ContextRegistry

/**
 * Keyword definition for {@code @CheckoutRecentSourceStep}
 */
def call(Map options) {
  ContextRegistry.registerDefaultContext(this)
  new CheckoutRecentSourceStep(options).run()
}
