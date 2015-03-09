/*
 *   Copyright (C) 2014-2015 GeorgH93
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package at.pcgamingfreaks.MarriageMaster.Bukkit.Network;

import net.minecraft.server.v1_8_R2.PacketPlayOutWorldParticles;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;

public class Effect_1_8_R2 extends EffectBase
{
	private static Class<?>	nmsEnumParticle;
	
	public void SpawnParticle(Location loc, Effects type, double visrange, int count, float random1, float random2, float random3, float random4) throws Exception
	{
		if (nmsEnumParticle == null)
		{
			nmsEnumParticle = getNMSClass("EnumParticle");
		}
		
		PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles();
		NMS.setValue(packet, "a", getEnum(nmsEnumParticle.getName() + "." + (type.getName().toUpperCase())));
		NMS.setValue(packet, "b", (float) loc.getX());
		NMS.setValue(packet, "c", (float) loc.getY());
		NMS.setValue(packet, "d", (float) loc.getZ());
		NMS.setValue(packet, "e", random1);
		NMS.setValue(packet, "f", random2);
		NMS.setValue(packet, "g", random3);
		NMS.setValue(packet, "h", random4);
		NMS.setValue(packet, "i", count);
		NMS.setValue(packet, "j", false);
		NMS.setValue(packet, "k", new int[]{});
		for(Entity entity : loc.getWorld().getEntities())
		{
			if(entity instanceof CraftPlayer)
			{
				if(entity.getLocation().getWorld().equals(loc.getWorld()) && entity.getLocation().distance(loc) < visrange)
				{
					((CraftPlayer)entity).getHandle().playerConnection.sendPacket(packet);
				}
			}
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Enum<?> getEnum(String enumFullName) {
		String[] x = enumFullName.split("\\.(?=[^\\.]+$)");
		if (x.length == 2) {
			String enumClassName = x[0];
			String enumName = x[1];
			try {
				Class<Enum> cl = (Class<Enum>) Class.forName(enumClassName);
				return Enum.valueOf(cl, enumName);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static Class<?> getNMSClass(String className) {
		String fullName = "net.minecraft.server.v1_8_R1." + className;
		Class<?> clazz = null;
		try {
			clazz = Class.forName(fullName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clazz;
	}
}
