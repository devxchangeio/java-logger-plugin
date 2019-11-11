/**
 * 
 */
package io.oneclicklabs.logger.plugin.log4j;

/**
 * @author karthy
 *
 */
public class FinalType<I>
{
  private I value;
  
  public void setValue(I value)
  {
    this.value = value;
  }
  
  public I getValue()
  {
    return (I)this.value;
  }
}
