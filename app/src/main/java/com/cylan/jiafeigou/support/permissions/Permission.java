package com.cylan.jiafeigou.support.permissions;

public class Permission {
    public final String name;
    public final boolean granted;

    public Permission(String name, boolean granted) {
        this.name = name;
        this.granted = granted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Permission that = (Permission) o;
        http:
//yf.cylan.com.cn:82/Garfield/zhongxing/Android/2017071917-5416/

        if (granted != that.granted) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (granted ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Permission{" +
                "name='" + name + '\'' +
                ", granted=" + granted +
                '}';
    }
}
