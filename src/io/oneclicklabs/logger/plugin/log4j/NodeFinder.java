/**
 * 
 */
package io.oneclicklabs.logger.plugin.log4j;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;

/**
 * @author karthy
 *
 * Sep 23, 2016
 */

public class NodeFinder
  extends GenericVisitor
{
  private int fStart;
  private int fEnd;
  private ASTNode fCoveringNode;
  private ASTNode fCoveredNode;
  
  public static ASTNode perform(ASTNode root, int start, int length)
  {
    NodeFinder finder = new NodeFinder(start, length);
    root.accept(finder);
    ASTNode result = finder.getCoveredNode();
    if ((result == null) || (result.getStartPosition() != start) || (result.getLength() != length)) {
      return finder.getCoveringNode();
    }
    return result;
  }
  
  public NodeFinder(int offset, int length)
  {
    super(true);
    this.fStart = offset;
    this.fEnd = (offset + length);
  }
  
  protected boolean visitNode(ASTNode node)
  {
    int nodeStart = node.getStartPosition();
    int nodeEnd = nodeStart + node.getLength();
    if ((nodeEnd < this.fStart) || (this.fEnd < nodeStart)) {
      return false;
    }
    if ((nodeStart <= this.fStart) && (this.fEnd <= nodeEnd)) {
      this.fCoveringNode = node;
    }
    if ((this.fStart <= nodeStart) && (nodeEnd <= this.fEnd))
    {
      if (this.fCoveringNode == node)
      {
        this.fCoveredNode = node;
        return true;
      }
      if (this.fCoveredNode == null) {
        this.fCoveredNode = node;
      }
      return false;
    }
    return true;
  }
  
  public ASTNode getCoveredNode()
  {
    return this.fCoveredNode;
  }
  
  public ASTNode getCoveringNode()
  {
    return this.fCoveringNode;
  }
}
