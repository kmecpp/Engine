package resonant.lib.grid

import resonant.lib.grid.node.NodeGrid

import scala.collection.convert.wrapAll._

/**
 * A grid that contains nodes where all nodes are interconnected
 *
 * @param N The type of node we can connect
 * @author Calclavia
 */
class GridNode[N <: NodeGrid[N]](node: Class[N]) extends Grid[N](node)
{
  /**
   * Rebuild the node list starting from the first node and recursively iterating through its connections.
   */
  def reconstruct(first: N)
  {
    //TODO: Reconstruct may be called MANY times unnecessarily multiple times. Add check to prevent extra calls
    //if (!getNodes.contains(first))
    {
      getNodes.clear()
      populate(first)

      getNodes.foreach(_.onGridReconstruct())
    }
  }

  /**
   * Populates the node list
   */
  protected def populate(node: N, prev: N = null.asInstanceOf[N])
  {
    if (!getNodes.contains(node) && isValidNode(node))
    {
      add(node)
      populateNode(node, prev)
      node.connections.foreach(n => populate(n, node))
    }
  }

  protected def populateNode(node: N, prev: N = null.asInstanceOf[N])
  {
    if (node.grid != this)
    {
      node.grid.remove(node)
      node.setGrid(this)
    }
  }

  def deconstruct(first: N)
  {
    remove(first)
    first.setGrid(null)

    if (getNodes.size() > 0)
      reconstruct(getNodes.head)
  }
}