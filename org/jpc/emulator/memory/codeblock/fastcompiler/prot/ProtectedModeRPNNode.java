/*
    JPC: A x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.0

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2009 Isis Innovation Limited

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

    Details (including contact information) can be found at: 

    www-jpc.physics.ox.ac.uk
*/

package org.jpc.emulator.memory.codeblock.fastcompiler.prot;

import org.jpc.emulator.memory.codeblock.fastcompiler.*;

/**
 * 
 * @author Chris Dennis
 */
public class ProtectedModeRPNNode extends RPNNode
{
    public ProtectedModeRPNNode(int id, MicrocodeNode parent)
    {
	super(id, parent);
    }

    protected Object[] getByteCodes()
    {
        if (getMicrocode() == -1)
            return ProtectedModeBytecodeFragments.pushCode(getID());
        
        if (hasImmediate())
            return ProtectedModeBytecodeFragments.getOperation(getID(), getMicrocode(), getX86Position(), getImmediate());
        else
            return ProtectedModeBytecodeFragments.getOperation(getID(), getMicrocode(), getX86Position());
    }

    public boolean hasExternalEffect()
    {
	if (getMicrocode() == -1)
	    return false;

	return ProtectedModeBytecodeFragments.hasExternalEffect(getID(), getMicrocode());
    }

    public boolean canThrowException()
    {
	if (getMicrocode() == -1)
	    return false;

	return ProtectedModeBytecodeFragments.hasExplicitThrow(getID(), getMicrocode()) || hasExternalEffect();
    }
}