import java.util.Random;
import java.io.Serializable;

public class BinarySearchTree implements Serializable
{
	//inner node class
	private class Node implements Serializable
	{
		private String key; 
		private long pointer;
		//private long poStringer;
		private Node left;
		private Node right;
		
		//node constructor
		private Node(String key, long pointer, Node left, Node right)
		{
			this.key = key;
			this.pointer = pointer;
			this.left = left;
			this.right = right;
		}
		
		public boolean remove(String value, Node parent) 
		{

            if (value.compareTo(this.key) < 0) 
            {

                  if (left != null)

                        return left.remove(value, this);

                  else

                        return false;

            } 
            else if (value.compareTo(this.key) > 0) 
            {

                  if (right != null)

                        return right.remove(value, this);

                  else

                        return false;

            } 
            else 
            {

                  if (left != null && right != null) {

                        this.key = right.minValue();

                        right.remove(this.key, this);

                  } else if (parent.left == this) {

                        parent.left = (left != null) ? left : right;

                  } else if (parent.right == this) {

                        parent.right = (left != null) ? left : right;

                  }

                  return true;

            }

		}
		
		public Node getLeft()
		{
			return left;
		}
		
		public Node getRight()
		{
			return right;
		}
		
		public String getKey()
		{
			return key;
		}
		
		public String minValue() 
		{
            if (left == null)
                  return key;
            else
                  return left.minValue();

		}
		
		public void setLeftChild(Node n)
		{
			left = n;
		}
	}
	
	//root of tree
	private Node root;
	//private Random generator;
	
	//constructor of tree
	public BinarySearchTree()
	{
		root = null;
		//generator = new Random(seed);
	}
	
	//searches for given Stringeger
	public boolean search(String key)
	{
		Node temp = root;				//start at root
		
		while (temp != null)			//search for key
		{
			//compare key with current node key
			if (key.compareTo(temp.key) == 0)		//key found
				return true;
			//else if (key2 < temp.key)	//search left subtree
			else if (key.compareTo(temp.key) < 0)	//search left subtree
				temp = temp.left;
			else						//search right subtree
				temp = temp.right;		
		}
		
		return false;					//key not found
	}
	
	//insert random number Stringo tree
	public boolean insert(String key, long pointer)//long pointer)
	{	
		//if tree is empty
		if (root == null)
		{
			//insert at root
			root = new Node(key, pointer, null, null);
			return true;
		}
		//otherwise call helper method
		else
			return insert(root, key, pointer);
	}
	
	//helper method for inserting key Stringo the tree
	private boolean insert(Node node, String key, long pointer)//long pointer)
	{
		//don't insert duplicates
		if (key.compareTo(node.key) == 0)
			return false;
		else if (key.compareTo(node.key) < 0)
		{
			if (node.left == null)
			{
				node.left = new Node(key, pointer, null, null);
				return true;
			}
			else
				return insert(node.left, key, pointer);
		}
		else
		{
			if (node.right == null)
			{
				node.right = new Node(key, pointer, null, null);
				return true;
			}
			else
				return insert(node.right, key, pointer);
		}
	}
	
	public boolean delete(String key)
	{
		if (root == null)
            return false;
        else 
        {
            if (root.getKey() == key) 
            {
                  Node auxRoot = new Node("",0, root,root);
                  auxRoot.setLeftChild(root);

                  boolean result = root.remove(key, auxRoot);

                  root = auxRoot.getLeft();

                  return result;

            }
            else 
            {
                  return root.remove(key, null);
            }

        }
	}
	
	
	public void printTree()
	{
		printTree(root);
	}
	
	public void printTree(Node node)
	{
		if(node != null)
		{
			printTree(node.left);
			System.out.println(node.key+" "+node.pointer);
			printTree(node.right);
		}
	}
	/*//finds height of subtree whose root is node
	private String height(Node node)
	{
		//tree is empty
		if (node == null)			
			return 0;
		//else compute and return recursively the greater height of
		//either the left or right subtree
		else
			return Math.max(height(node.left) + 1, height(node.right) + 1);
	}
	
	//returns the number of nodes in the tree
	public String countNodes()
	{
		return countNodes(root);
	}*/
	
	//helper method that counts all nodes in the tree recursively
	/*private String countNodes(Node node)
	{
		Node temp = node;
		
		//base case
		if (temp == null)
			return 0;
		//increment and return node count recursively
		else
			return countNodes(temp.left) + countNodes(temp.right) + 1;
	}*/
	
	/*//returns the height of entire tree
	public String height()
	{
		return height(root);
	}*/

}
